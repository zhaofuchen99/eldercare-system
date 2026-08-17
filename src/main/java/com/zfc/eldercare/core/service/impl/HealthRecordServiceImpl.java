package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.HealthRecordDTO;
import com.zfc.eldercare.core.entity.HealthGuidance;
import com.zfc.eldercare.core.entity.HealthRecord;
import com.zfc.eldercare.core.entity.Message;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.HealthGuidanceMapper;
import com.zfc.eldercare.core.mapper.HealthRecordMapper;
import com.zfc.eldercare.core.mapper.MessageMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.HealthRecordService;
import com.zfc.eldercare.core.vo.HealthRecordVO;
import com.zfc.eldercare.core.vo.HealthTrendVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 健康记录服务实现（详细设计文档 5.2）。
 * 注意：健康建议目前使用模板生成；接入 DeepSeek 个性化建议将在 5.4 AI 对话模块中实现后替换。
 */
@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    /** 月份分组格式 */
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    /** 健康提醒站内消息标题 */
    private static final String REMINDER_TITLE = "健康提醒";

    // ========== 指标阈值（详细设计文档 5.2 健康提醒触发条件） ==========
    private static final BigDecimal BLOOD_SUGAR_LOW = new BigDecimal("3.9");
    private static final BigDecimal BLOOD_SUGAR_HIGH = new BigDecimal("6.1");
    private static final BigDecimal BMI_LOW = new BigDecimal("18.5");
    private static final BigDecimal BMI_HIGH = new BigDecimal("24.0");

    private final HealthRecordMapper healthRecordMapper;
    private final HealthGuidanceMapper healthGuidanceMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Long record(Long userId, HealthRecordDTO dto) {
        // 至少填写一项指标
        if (dto.systolic() == null && dto.diastolic() == null && dto.bloodSugar() == null
                && dto.heartRate() == null && dto.weight() == null) {
            throw new BusinessException("请至少填写一项健康指标");
        }

        HealthRecord record = new HealthRecord();
        record.setUserId(userId);
        record.setSystolic(dto.systolic());
        record.setDiastolic(dto.diastolic());
        record.setBloodSugar(dto.bloodSugar());
        record.setHeartRate(dto.heartRate());
        record.setWeight(dto.weight());
        record.setMemo(dto.memo());
        record.setRecordedTime(LocalDateTime.now());

        // 身高（cm）来自用户档案，BMI = 体重(kg) / (身高(m))^2
        User user = userMapper.selectById(userId);
        if (record.getWeight() != null && user != null && user.getHeight() != null
                && user.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightM = user.getHeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            record.setBmi(record.getWeight().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP));
        }

        healthRecordMapper.insert(record);

        triggerHealthReminders(userId, record);

        return record.getId();
    }

    @Override
    public PageVO<HealthRecordVO> pageByUser(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<HealthRecord> list = healthRecordMapper.selectByUserId(userId);
        PageInfo<HealthRecord> pageInfo = new PageInfo<>(list);
        List<HealthRecordVO> voList = list.stream().map(HealthRecordVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public HealthTrendVO trend(Long userId, String indicator) {
        LocalDateTime start = LocalDateTime.now().minusMonths(6);
        List<HealthRecord> records = healthRecordMapper.selectSince(userId, start);

        Map<String, Map<String, TrendAccumulator>> byIndicator = new LinkedHashMap<>();
        for (HealthRecord r : records) {
            collect(byIndicator, "SYSTOLIC", r.getSystolic() == null ? null : BigDecimal.valueOf(r.getSystolic()), r, indicator);
            collect(byIndicator, "DIASTOLIC", r.getDiastolic() == null ? null : BigDecimal.valueOf(r.getDiastolic()), r, indicator);
            collect(byIndicator, "BLOOD_SUGAR", r.getBloodSugar(), r, indicator);
            collect(byIndicator, "HEART_RATE", r.getHeartRate() == null ? null : BigDecimal.valueOf(r.getHeartRate()), r, indicator);
            collect(byIndicator, "BMI", r.getBmi(), r, indicator);
        }

        Map<String, List<TrendPointVO>> data = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, TrendAccumulator>> e : byIndicator.entrySet()) {
            List<TrendPointVO> points = new ArrayList<>();
            for (Map.Entry<String, TrendAccumulator> m : e.getValue().entrySet()) {
                TrendAccumulator acc = m.getValue();
                points.add(new TrendPointVO(m.getKey(),
                        acc.avg(),
                        acc.max.setScale(1, RoundingMode.HALF_UP),
                        acc.min.setScale(1, RoundingMode.HALF_UP)));
            }
            data.put(e.getKey(), points);
        }
        return new HealthTrendVO(data);
    }

    /** 收集某指标某月的聚合统计 */
    private void collect(Map<String, Map<String, TrendAccumulator>> byIndicator, String key,
                         BigDecimal value, HealthRecord record, String onlyIndicator) {
        if (value == null) {
            return;
        }
        if (StringUtils.hasText(onlyIndicator) && !onlyIndicator.equals(key)) {
            return;
        }
        String month = MONTH_FMT.format(record.getRecordedTime());
        byIndicator.computeIfAbsent(key, k -> new TreeMap<>())
                .computeIfAbsent(month, k -> new TrendAccumulator())
                .add(value);
    }

    /** 逐指标判定超标并生成健康指导 + 站内消息（同一指标当日只提醒一次） */
    private void triggerHealthReminders(Long userId, HealthRecord record) {
        List<Abnormal> abnormals = new ArrayList<>();

        if (record.getSystolic() != null && (record.getSystolic() < 90 || record.getSystolic() >= 140)) {
            abnormals.add(new Abnormal("SYSTOLIC", "DAILY",
                    "您的收缩压为 " + record.getSystolic() + " mmHg，正常范围 90-139 mmHg。建议规律作息、减少钠盐摄入，持续偏高请及时就医。"));
        }
        if (record.getDiastolic() != null && (record.getDiastolic() < 60 || record.getDiastolic() >= 90)) {
            abnormals.add(new Abnormal("DIASTOLIC", "DAILY",
                    "您的舒张压为 " + record.getDiastolic() + " mmHg，正常范围 60-89 mmHg。建议保持规律作息、清淡饮食，必要时咨询医生。"));
        }
        if (record.getBloodSugar() != null
                && (record.getBloodSugar().compareTo(BLOOD_SUGAR_LOW) < 0
                || record.getBloodSugar().compareTo(BLOOD_SUGAR_HIGH) > 0)) {
            abnormals.add(new Abnormal("BLOOD_SUGAR", "DIET",
                    "您的空腹血糖为 " + record.getBloodSugar() + " mmol/L，正常范围 3.9-6.1 mmol/L。建议控制含糖饮食、少食多餐，异常请复查血糖。"));
        }
        if (record.getHeartRate() != null && (record.getHeartRate() < 60 || record.getHeartRate() > 100)) {
            abnormals.add(new Abnormal("HEART_RATE", "EXERCISE",
                    "您的心率为 " + record.getHeartRate() + " 次/分，正常范围 60-100 次/分。建议适量运动、避免过度劳累，持续异常请及时就医。"));
        }
        // 体重阈值按 BMI 派生（文档：体重"同 BMI"），合并为 BMI 指标，避免同一问题重复推送
        if (record.getBmi() != null && (record.getBmi().compareTo(BMI_LOW) < 0 || record.getBmi().compareTo(BMI_HIGH) >= 0)) {
            abnormals.add(new Abnormal("BMI", "DATA_SUMMARY",
                    "您的 BMI 指数为 " + record.getBmi() + "，正常范围 18.5-23.9。建议均衡饮食并配合适当运动，保持健康体重。"));
        }

        for (Abnormal a : abnormals) {
            // 同一指标当日多次超标只推送一次（去重：user_id + indicator + 当天，文档 5.2）
            HealthGuidance existed = healthGuidanceMapper.selectTodayByIndicator(userId, a.indicator);
            if (existed != null) {
                continue;
            }
            HealthGuidance guidance = new HealthGuidance();
            guidance.setUserId(userId);
            guidance.setType(a.type);
            guidance.setIndicator(a.indicator);
            guidance.setContent(a.content);
            healthGuidanceMapper.insert(guidance);

            Message message = new Message();
            message.setUserId(userId);
            message.setTitle(REMINDER_TITLE);
            message.setContent(a.content);
            message.setType("HEALTH_REMINDER");
            messageMapper.insert(message);
        }
    }

    /** 阈值命中项 */
    private record Abnormal(String indicator, String type, String content) {
    }

    /** 月度聚合累加器 */
    private static class TrendAccumulator {
        private BigDecimal sum = BigDecimal.ZERO;
        private int count = 0;
        private BigDecimal max;
        private BigDecimal min;

        void add(BigDecimal v) {
            sum = sum.add(v);
            count++;
            if (max == null || v.compareTo(max) > 0) {
                max = v;
            }
            if (min == null || v.compareTo(min) < 0) {
                min = v;
            }
        }

        BigDecimal avg() {
            return sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        }
    }
}
