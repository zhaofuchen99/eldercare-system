package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.AppointmentStatusDTO;
import com.zfc.eldercare.core.dto.PackageDTO;
import com.zfc.eldercare.core.dto.SlotBatchDTO;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.vo.AppointmentVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PackageVO;
import com.zfc.eldercare.core.vo.SlotVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端体检管理接口（/api/admin/appointment，详细设计文档 5.5 / 7.2）。
 */
@RestController
@RequestMapping("/api/admin/appointment")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    // ========== 套餐管理 ==========

    /** 新增套餐 */
    @PostMapping("/package")
    public Result<Long> createPackage(@Valid @RequestBody PackageDTO dto) {
        return Result.success("新增成功", appointmentService.createPackage(dto));
    }

    /** 更新套餐 */
    @PutMapping("/package/{id}")
    public Result<Void> updatePackage(@PathVariable Long id, @Valid @RequestBody PackageDTO dto) {
        appointmentService.updatePackage(id, dto);
        return Result.success("更新成功", null);
    }

    /** 删除套餐（逻辑删除） */
    @DeleteMapping("/package/{id}")
    public Result<Void> deletePackage(@PathVariable Long id) {
        appointmentService.deletePackage(id);
        return Result.success("删除成功", null);
    }

    /** 套餐分页 */
    @GetMapping("/package/page")
    public Result<PageVO<PackageVO>> packagePage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(appointmentService.packagePage(page, size));
    }

    /** 套餐详情 */
    @GetMapping("/package/{id}")
    public Result<PackageVO> packageDetail(@PathVariable Long id) {
        return Result.success(appointmentService.packageDetail(id));
    }

    // ========== 时段管理 ==========

    /** 批量生成预约时段（dates × timeRanges，已存在自动跳过） */
    @PostMapping("/slot/batch")
    public Result<Void> batchCreateSlots(@Valid @RequestBody SlotBatchDTO dto) {
        appointmentService.batchCreateSlots(dto);
        return Result.success("生成成功", null);
    }

    /** 时段查询（可按套餐/日期过滤） */
    @GetMapping("/slot")
    public Result<List<SlotVO>> slots(@RequestParam(required = false) Long packageId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(appointmentService.adminSlots(packageId, date));
    }

    // ========== 预约管理 ==========

    /** 预约分页（可按状态/用户/套餐/时段日期过滤） */
    @GetMapping("/page")
    public Result<PageVO<AppointmentVO>> page(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) Long packageId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointDate,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(appointmentService.appointmentPage(status, userId, packageId, appointDate, page, size));
    }

    /** 处理预约状态（确认 CONFIRMED / 取消 CANCELED） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AppointmentStatusDTO dto) {
        appointmentService.updateStatus(id, dto.status());
        return Result.success("操作成功", null);
    }

    /** 上传体检报告（PDF ≤20MB，自动置为已完成） */
    @PostMapping("/{id}/report")
    public Result<Void> uploadReport(@PathVariable Long id,
                                     @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal Long adminId) {
        appointmentService.uploadReport(id, file, adminId);
        return Result.success("上传成功", null);
    }
}
