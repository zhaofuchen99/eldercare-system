package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.AppointmentCreateDTO;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.vo.AppointmentVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PackageVO;
import com.zfc.eldercare.core.vo.SlotVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 会员端体检预约接口（/api/member/appointment，详细设计文档 5.5 / 7.2）。
 */
@RestController
@RequestMapping("/api/member/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** 套餐列表（仅启用） */
    @GetMapping("/packages")
    public Result<List<PackageVO>> packages() {
        return Result.success(appointmentService.packages());
    }

    /** 可预约时段（可按日期过滤） */
    @GetMapping("/packages/{id}/slots")
    public Result<List<SlotVO>> slots(@PathVariable Long id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(appointmentService.availableSlots(id, date));
    }

    /** 提交预约（自动扣减积分并生成消费流水） */
    @PostMapping
    public Result<Long> create(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody AppointmentCreateDTO dto) {
        return Result.success("预约成功", appointmentService.create(userId, dto));
    }

    /** 取消预约（退还积分、释放名额） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        appointmentService.cancel(userId, id);
        return Result.success("取消成功", null);
    }

    /** 我的预约分页 */
    @GetMapping
    public Result<PageVO<AppointmentVO>> myAppointments(@AuthenticationPrincipal Long userId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(appointmentService.myAppointments(userId, page, size));
    }

    /** 体检报告下载（5 分钟签名链接，仅本人或管理员可访问，文档 5.5） */
    @GetMapping("/report/download")
    public ResponseEntity<byte[]> downloadReport(@AuthenticationPrincipal Long userId,
                                                 @RequestParam Long appointmentId,
                                                 @RequestParam long expires,
                                                 @RequestParam String sign) {
        byte[] data = appointmentService.downloadReport(userId, appointmentId, expires, sign);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/pdf"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("体检报告.pdf", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
