package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.AppointmentCreateDTO;
import com.zfc.eldercare.core.dto.PackageDTO;
import com.zfc.eldercare.core.dto.SlotBatchDTO;
import com.zfc.eldercare.core.vo.AppointmentVO;
import com.zfc.eldercare.core.vo.PackageVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.SlotVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 体检预约服务（详细设计文档 5.5）。
 */
public interface AppointmentService {

    // ========== 会员端 ==========

    /** 启用套餐列表 */
    List<PackageVO> packages();

    /** 可预约时段查询（仅未来日期且可预约） */
    List<SlotVO> availableSlots(Long packageId, LocalDate date);

    /** 提交预约（并发控制 + 积分 FIFO 扣减 + 名额占用，文档 5.5 核心流程） */
    Long create(Long userId, AppointmentCreateDTO dto);

    /** 取消预约（退还积分 + 名额释放，仅未完成且未过期可取消） */
    void cancel(Long userId, Long appointmentId);

    /** 我的预约分页 */
    PageVO<AppointmentVO> myAppointments(Long userId, int page, int size);

    /** 下载体检报告（签名校验 + 权限校验：仅预约本人或管理员） */
    byte[] downloadReport(Long userId, Long appointmentId, long expires, String sign);

    // ========== 管理端 ==========

    /** 新增套餐 */
    Long createPackage(PackageDTO dto);

    /** 更新套餐 */
    void updatePackage(Long id, PackageDTO dto);

    /** 删除套餐（逻辑删除） */
    void deletePackage(Long id);

    /** 套餐分页 */
    PageVO<PackageVO> packagePage(int page, int size);

    /** 套餐详情 */
    PackageVO packageDetail(Long id);

    /** 批量生成预约时段（dates × timeRanges，已存在自动跳过） */
    void batchCreateSlots(SlotBatchDTO dto);

    /** 时段查询（管理端，可按套餐/日期过滤） */
    List<SlotVO> adminSlots(Long packageId, LocalDate date);

    /** 预约分页（管理端，可按状态/用户/套餐/时段日期过滤） */
    PageVO<AppointmentVO> appointmentPage(String status, Long userId, Long packageId,
                                          LocalDate appointDate, int page, int size);

    /** 处理预约状态：CONFIRMED（待确认→已确认）/ CANCELED（取消并退还积分/名额） */
    void updateStatus(Long appointmentId, String status);

    /** 上传体检报告（PDF ≤20MB，自动置为已完成） */
    void uploadReport(Long appointmentId, MultipartFile file, Long adminId);
}
