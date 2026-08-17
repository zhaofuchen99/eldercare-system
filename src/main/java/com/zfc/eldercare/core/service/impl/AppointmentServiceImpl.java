package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.AppointmentCreateDTO;
import com.zfc.eldercare.core.dto.PackageDTO;
import com.zfc.eldercare.core.dto.SlotBatchDTO;
import com.zfc.eldercare.core.entity.Appointment;
import com.zfc.eldercare.core.entity.AppointmentPackage;
import com.zfc.eldercare.core.entity.AppointmentSlot;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.AppointmentMapper;
import com.zfc.eldercare.core.mapper.AppointmentPackageMapper;
import com.zfc.eldercare.core.mapper.AppointmentSlotMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.service.FileStorageService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.service.SmsService;
import com.zfc.eldercare.core.vo.AppointmentVO;
import com.zfc.eldercare.core.vo.PackageVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.SlotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 体检预约服务实现（详细设计文档 5.5 / 9.3 并发控制 / 5.8 积分 FIFO）。
 * 预约提交与取消为同事务操作，积分扣减/名额占用任一步失败整体回滚；短信通知做容错。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String ROLE_ADMIN = "ADMIN";
    /** 报告下载链接有效期（秒）：5 分钟（文档 5.5 防盗链） */
    private static final long REPORT_LINK_TTL_SECONDS = 300L;

    private final AppointmentMapper appointmentMapper;
    private final AppointmentPackageMapper appointmentPackageMapper;
    private final AppointmentSlotMapper appointmentSlotMapper;
    private final UserMapper userMapper;
    private final PointsService pointsService;
    private final FileStorageService fileStorageService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    /** 报告下载签名密钥（配置 eldercare.report-download-secret，生产用环境变量注入） */
    @Value("${eldercare.report-download-secret:eldercare-report-download-secret-2026}")
    private String downloadSecret;

    // ========== 会员端 ==========

    @Override
    public List<PackageVO> packages() {
        return appointmentPackageMapper.selectEnabled().stream()
                .map(p -> PackageVO.from(p, parseItems(p.getItems())))
                .toList();
    }

    @Override
    public List<SlotVO> availableSlots(Long packageId, LocalDate date) {
        return appointmentSlotMapper.selectAvailable(packageId, date).stream()
                .map(s -> SlotVO.from(s, null))
                .toList();
    }

    @Override
    @Transactional
    public Long create(Long userId, AppointmentCreateDTO dto) {
        // 1. 校验用户状态正常（未禁用）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，无法预约");
        }

        // 2. 校验时段可预约（未过期）
        AppointmentSlot slot = appointmentSlotMapper.selectById(dto.slotId());
        if (slot == null) {
            throw new BusinessException(404, "预约时段不存在");
        }
        if (!STATUS_AVAILABLE.equals(slot.getStatus())) {
            throw new BusinessException(409, "该时段暂不可预约");
        }
        if (slot.getAppointDate().isBefore(LocalDate.now())) {
            throw new BusinessException(409, "该时段已过期");
        }

        // 3. 校验套餐已上架
        AppointmentPackage pkg = appointmentPackageMapper.selectById(slot.getPackageId());
        if (pkg == null || !STATUS_ENABLED.equals(pkg.getStatus())) {
            throw new BusinessException(409, "套餐已下架");
        }

        // 4. 先建预约记录（拿到 ID 供消费流水 ref_id；后续任一步失败整体回滚）
        Appointment appt = new Appointment();
        appt.setUserId(userId);
        appt.setSlotId(slot.getId());
        appt.setPackageId(pkg.getId());
        appt.setStatus(STATUS_PENDING);
        appointmentMapper.insert(appt);

        // 5. 原子扣减积分（不足回滚）+ FIFO 消费流水（文档 5.5 步骤 4 / 5.8 FIFO）
        int price = pkg.getPrice() == null ? 0 : pkg.getPrice();
        if (price > 0) {
            int rows = userMapper.deductPoints(userId, price);
            if (rows == 0) {
                throw new BusinessException(409, "积分不足，该套餐需 " + price + " 积分");
            }
            pointsService.consumeAppointment(userId, appt.getId(), price);
        }

        // 6. 原子占用名额（已满回滚；current_count < max_count 行锁保证并发安全）
        int cnt = appointmentSlotMapper.incrementCount(slot.getId());
        if (cnt == 0) {
            throw new BusinessException(409, "该时段名额已满");
        }

        // 7. 预约成功短信通知（容错：失败仅记日志，不影响主事务结果，文档 5.5）
        try {
            smsService.sendText(user.getPhone(),
                    "您已成功预约【" + pkg.getName() + "】" + slot.getAppointDate() + " " + slot.getTimeRange()
                            + "，共 " + price + " 积分，请按时参加体检。");
        } catch (Exception e) {
            log.warn("预约成功短信发送失败，appointmentId={}", appt.getId(), e);
        }
        return appt.getId();
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long appointmentId) {
        Appointment appt = requireOwned(appointmentId, userId);
        cancelInternal(appt);
    }

    @Override
    public PageVO<AppointmentVO> myAppointments(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<Appointment> list = appointmentMapper.selectPageByUserId(userId);
        PageInfo<Appointment> pageInfo = new PageInfo<>(list);
        List<AppointmentVO> voList = toVOList(list);
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public byte[] downloadReport(Long userId, Long appointmentId, long expires, String sign) {
        // 签名校验（防止盗链）
        String expected = md5(appointmentId + ":" + expires + ":" + downloadSecret);
        if (sign == null || !expected.equals(sign)) {
            throw new BusinessException(403, "下载链接无效");
        }
        if (expires < System.currentTimeMillis() / 1000) {
            throw new BusinessException(403, "下载链接已过期，请重新获取");
        }
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException(404, "预约不存在");
        }
        // 权限校验：仅预约本人或管理员可访问（文档 5.5）
        if (!appt.getUserId().equals(userId)) {
            User user = userMapper.selectById(userId);
            if (user == null || !ROLE_ADMIN.equals(user.getRole())) {
                throw new BusinessException(403, "无权访问该体检报告");
            }
        }
        if (!StringUtils.hasText(appt.getReportUrl())) {
            throw new BusinessException(404, "该预约暂无体检报告");
        }
        return fileStorageService.loadReport(appt.getReportUrl());
    }

    // ========== 管理端 ==========

    @Override
    public Long createPackage(PackageDTO dto) {
        if (!StringUtils.hasText(dto.name())) {
            throw new BusinessException("套餐名称不能为空");
        }
        AppointmentPackage pkg = new AppointmentPackage();
        pkg.setName(dto.name());
        pkg.setCoverUrl(dto.coverUrl());
        pkg.setDescription(dto.description());
        pkg.setPrice(dto.price() == null ? 0 : dto.price());
        pkg.setSuitablePeople(dto.suitablePeople());
        pkg.setItems(writeItems(dto.items()));
        pkg.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        appointmentPackageMapper.insert(pkg);
        return pkg.getId();
    }

    @Override
    public void updatePackage(Long id, PackageDTO dto) {
        if (appointmentPackageMapper.selectById(id) == null) {
            throw new BusinessException(404, "套餐不存在");
        }
        AppointmentPackage pkg = new AppointmentPackage();
        pkg.setId(id);
        pkg.setName(dto.name());
        pkg.setCoverUrl(dto.coverUrl());
        pkg.setDescription(dto.description());
        pkg.setPrice(dto.price());
        pkg.setSuitablePeople(dto.suitablePeople());
        pkg.setItems(dto.items() == null ? null : writeItems(dto.items()));
        pkg.setStatus(dto.status());
        appointmentPackageMapper.update(pkg);
    }

    @Override
    public void deletePackage(Long id) {
        if (appointmentPackageMapper.selectById(id) == null) {
            throw new BusinessException(404, "套餐不存在");
        }
        appointmentPackageMapper.delete(id);
    }

    @Override
    public PageVO<PackageVO> packagePage(int page, int size) {
        PageHelper.startPage(page, size);
        List<AppointmentPackage> list = appointmentPackageMapper.selectPage();
        PageInfo<AppointmentPackage> pageInfo = new PageInfo<>(list);
        List<PackageVO> voList = list.stream()
                .map(p -> PackageVO.from(p, parseItems(p.getItems())))
                .toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public PackageVO packageDetail(Long id) {
        AppointmentPackage pkg = appointmentPackageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException(404, "套餐不存在");
        }
        return PackageVO.from(pkg, parseItems(pkg.getItems()));
    }

    @Override
    public void batchCreateSlots(SlotBatchDTO dto) {
        AppointmentPackage pkg = appointmentPackageMapper.selectById(dto.packageId());
        if (pkg == null) {
            throw new BusinessException(404, "套餐不存在");
        }
        int maxCount = dto.maxCount() == null ? 10 : dto.maxCount();
        // 已存在的 日期+时间段 自动跳过，避免重复生成
        Set<String> existing = appointmentSlotMapper.selectByPackage(dto.packageId(), null).stream()
                .map(s -> s.getAppointDate() + "|" + s.getTimeRange())
                .collect(Collectors.toSet());

        List<AppointmentSlot> slots = new ArrayList<>();
        for (LocalDate date : dto.dates()) {
            for (String range : dto.timeRanges()) {
                if (existing.contains(date + "|" + range)) {
                    continue;
                }
                AppointmentSlot slot = new AppointmentSlot();
                slot.setPackageId(dto.packageId());
                slot.setAppointDate(date);
                slot.setTimeRange(range);
                slot.setMaxCount(maxCount);
                slot.setCurrentCount(0);
                slot.setStatus(STATUS_AVAILABLE);
                slots.add(slot);
            }
        }
        if (!slots.isEmpty()) {
            appointmentSlotMapper.insertBatch(slots);
        }
    }

    @Override
    public List<SlotVO> adminSlots(Long packageId, LocalDate date) {
        List<AppointmentSlot> slots = appointmentSlotMapper.selectByPackage(packageId, date);
        if (slots.isEmpty()) {
            return List.of();
        }
        Set<Long> pkgIds = slots.stream().map(AppointmentSlot::getPackageId).collect(Collectors.toSet());
        Map<Long, String> pkgNames = appointmentPackageMapper.selectByIds(new ArrayList<>(pkgIds)).stream()
                .collect(Collectors.toMap(AppointmentPackage::getId, AppointmentPackage::getName, (a, b) -> a));
        return slots.stream()
                .map(s -> SlotVO.from(s, pkgNames.get(s.getPackageId())))
                .toList();
    }

    @Override
    public PageVO<AppointmentVO> appointmentPage(String status, Long userId, Long packageId,
                                                LocalDate appointDate, int page, int size) {
        PageHelper.startPage(page, size);
        List<Appointment> list = appointmentMapper.selectPage(status, userId, packageId, appointDate);
        PageInfo<Appointment> pageInfo = new PageInfo<>(list);
        List<AppointmentVO> voList = toVOList(list);
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    @Transactional
    public void updateStatus(Long appointmentId, String status) {
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (STATUS_CONFIRMED.equals(status)) {
            if (!STATUS_PENDING.equals(appt.getStatus())) {
                throw new BusinessException(409, "仅待确认的预约可确认");
            }
            appointmentMapper.updateStatus(appointmentId, STATUS_CONFIRMED);
        } else if (STATUS_CANCELED.equals(status)) {
            cancelInternal(appt);
        } else {
            throw new BusinessException(400, "不支持的状态变更");
        }
    }

    @Override
    @Transactional
    public void uploadReport(Long appointmentId, MultipartFile file, Long adminId) {
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (STATUS_CANCELED.equals(appt.getStatus())) {
            throw new BusinessException(409, "已取消的预约不能上传报告");
        }
        String relativePath = fileStorageService.storeReport(file);
        String originalName = file.getOriginalFilename();
        appointmentMapper.uploadReport(appointmentId, relativePath,
                StringUtils.hasText(originalName) ? originalName : "体检报告.pdf", adminId);
    }

    // ========== 私有辅助 ==========

    /** 取消核心逻辑：退还积分（按原批次）→ 释放名额 → 置为已取消 → 短信容错（文档 5.5 / 5.8） */
    private void cancelInternal(Appointment appt) {
        if (STATUS_COMPLETED.equals(appt.getStatus())) {
            throw new BusinessException(409, "已完成预约不可取消");
        }
        if (STATUS_CANCELED.equals(appt.getStatus())) {
            throw new BusinessException(409, "该预约已取消");
        }
        // 未过期才可取消（时段日期今天及以后）
        AppointmentSlot slot = appointmentSlotMapper.selectById(appt.getSlotId());
        if (slot == null) {
            throw new BusinessException(404, "预约时段不存在");
        }
        if (slot.getAppointDate().isBefore(LocalDate.now())) {
            throw new BusinessException(409, "该预约时段已过期，不可取消");
        }

        pointsService.refundAppointment(appt.getUserId(), appt.getId());
        appointmentSlotMapper.decrementCount(slot.getId());
        appointmentMapper.cancelById(appt.getId());

        AppointmentPackage pkg = appointmentPackageMapper.selectById(appt.getPackageId());
        User user = userMapper.selectById(appt.getUserId());
        try {
            if (user != null) {
                smsService.sendText(user.getPhone(),
                        "您的体检预约【" + (pkg == null ? "" : pkg.getName()) + "】"
                                + slot.getAppointDate() + " " + slot.getTimeRange() + " 已取消，积分已退回。");
            }
        } catch (Exception e) {
            log.warn("取消预约短信发送失败，appointmentId={}", appt.getId(), e);
        }
    }

    /** 校验预约归属并返回 */
    private Appointment requireOwned(Long appointmentId, Long userId) {
        Appointment appt = appointmentMapper.selectById(appointmentId);
        if (appt == null) {
            throw new BusinessException(404, "预约不存在");
        }
        if (!appt.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该预约");
        }
        return appt;
    }

    /** 预约记录批量组装 VO（批量查套餐/时段/用户，避免 N+1） */
    private List<AppointmentVO> toVOList(List<Appointment> appts) {
        if (appts.isEmpty()) {
            return List.of();
        }
        List<Long> pkgIds = appts.stream().map(Appointment::getPackageId).distinct().toList();
        List<Long> slotIds = appts.stream().map(Appointment::getSlotId).distinct().toList();
        List<Long> userIds = appts.stream().map(Appointment::getUserId).distinct().toList();

        Map<Long, AppointmentPackage> pkgMap = toMap(
                appointmentPackageMapper.selectByIds(pkgIds), AppointmentPackage::getId);
        Map<Long, AppointmentSlot> slotMap = toMap(
                appointmentSlotMapper.selectByIds(slotIds), AppointmentSlot::getId);
        Map<Long, User> userMap = toMap(userMapper.selectByIds(userIds), User::getId);

        return appts.stream().map(a -> {
            AppointmentPackage pkg = pkgMap.get(a.getPackageId());
            AppointmentSlot slot = slotMap.get(a.getSlotId());
            User user = userMap.get(a.getUserId());
            return new AppointmentVO(
                    a.getId(), a.getUserId(), a.getSlotId(), a.getPackageId(), a.getStatus(),
                    pkg == null ? null : pkg.getName(),
                    pkg == null ? null : pkg.getPrice(),
                    slot == null ? null : slot.getAppointDate(),
                    slot == null ? null : slot.getTimeRange(),
                    a.getOriginalFilename(), a.getReportUploadTime(), a.getCreateTime(),
                    user == null ? null : user.getRealName(),
                    user == null ? null : user.getPhone(),
                    buildSignedReportUrl(a));
        }).toList();
    }

    /** 按 key 建映射；调用方保证 key 不重复（按 distinct id 批量查询） */
    private <T, K> Map<K, T> toMap(List<T> list, Function<T, K> keyFn) {
        return list.stream().collect(Collectors.toMap(keyFn, Function.identity()));
    }

    /** 生成 5 分钟有效的报告下载签名链接（文档 5.5 防盗链） */
    private String buildSignedReportUrl(Appointment appt) {
        if (!StringUtils.hasText(appt.getReportUrl())) {
            return null;
        }
        long expires = System.currentTimeMillis() / 1000 + REPORT_LINK_TTL_SECONDS;
        String sign = md5(appt.getId() + ":" + expires + ":" + downloadSecret);
        return "/api/member/appointment/report/download?appointmentId=" + appt.getId()
                + "&expires=" + expires + "&sign=" + sign;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    private List<String> parseItems(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("解析套餐项目 JSON 失败，items={}", json);
            return List.of();
        }
    }

    private String writeItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new BusinessException("套餐项目格式错误");
        }
    }
}
