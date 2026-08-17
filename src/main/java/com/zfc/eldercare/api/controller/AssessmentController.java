package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.AssessmentSubmitDTO;
import com.zfc.eldercare.core.service.AssessmentService;
import com.zfc.eldercare.core.vo.AssessmentResultDetailVO;
import com.zfc.eldercare.core.vo.AssessmentResultListVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireDetailVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员端健康评测接口（/api/member/assessment，详细设计文档 7.2）。
 */
@RestController
@RequestMapping("/api/member/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    /** 已发布问卷列表 */
    @GetMapping("/questionnaires")
    public Result<List<MemberQuestionnaireVO>> questionnaires() {
        return Result.success(assessmentService.publishedQuestionnaires());
    }

    /** 问卷详情（含题目，用于答题） */
    @GetMapping("/questionnaire/{id}")
    public Result<MemberQuestionnaireDetailVO> questionnaire(@PathVariable Long id) {
        return Result.success(assessmentService.questionnaireDetail(id));
    }

    /** 提交评测 */
    @PostMapping("/submit")
    public Result<Long> submit(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody AssessmentSubmitDTO dto) {
        return Result.success("提交成功", assessmentService.submit(userId, dto));
    }

    /** 评测历史 */
    @GetMapping("/history")
    public Result<PageVO<AssessmentResultListVO>> history(@AuthenticationPrincipal Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(assessmentService.history(userId, page, size));
    }

    /** 评测报告详情 */
    @GetMapping("/result/{id}")
    public Result<AssessmentResultDetailVO> result(@AuthenticationPrincipal Long userId,
                                                   @PathVariable Long id) {
        return Result.success(assessmentService.resultDetail(userId, id));
    }
}
