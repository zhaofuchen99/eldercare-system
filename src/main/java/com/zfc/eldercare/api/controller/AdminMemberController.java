package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.MemberLevelDTO;
import com.zfc.eldercare.core.dto.MemberPointsDTO;
import com.zfc.eldercare.core.dto.ResetPasswordDTO;
import com.zfc.eldercare.core.service.MemberService;
import com.zfc.eldercare.core.vo.MemberVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端会员管理接口（/api/admin/members，详细设计文档 5.10 / 7.2）。
 * 列表、详情、启用、禁用、等级调整、积分调整、重置密码。
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    /** 会员分页（可按关键字/状态/等级筛选） */
    @GetMapping
    public Result<PageVO<MemberVO>> page(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String memberLevel,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.success(memberService.memberPage(keyword, status, memberLevel, page, size));
    }

    /** 会员详情（基本信息 + 积分） */
    @GetMapping("/{id}")
    public Result<MemberVO> detail(@PathVariable Long id) {
        return Result.success(memberService.memberDetail(id));
    }

    /** 启用会员 */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        memberService.enable(id);
        return Result.success("操作成功", null);
    }

    /** 禁用会员 */
    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        memberService.disable(id);
        return Result.success("操作成功", null);
    }

    /** 会员等级调整 */
    @PutMapping("/{id}/level")
    public Result<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody MemberLevelDTO dto) {
        memberService.updateLevel(id, dto.memberLevel());
        return Result.success("操作成功", null);
    }

    /** 积分手动调整（delta 正=调增，负=调减），返回调整后余额 */
    @PutMapping("/{id}/points")
    public Result<Integer> adjustPoints(@PathVariable Long id, @Valid @RequestBody MemberPointsDTO dto) {
        return Result.success("调整成功", memberService.adjustPoints(id, dto.delta()));
    }

    /** 重置密码（可指定或系统生成，强制所有设备下线），返回最终密码 */
    @PutMapping("/{id}/password")
    public Result<String> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        return Result.success("重置成功", memberService.resetPassword(id, dto.password()));
    }
}
