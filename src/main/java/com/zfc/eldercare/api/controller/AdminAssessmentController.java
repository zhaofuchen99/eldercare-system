package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.QuestionDTO;
import com.zfc.eldercare.core.dto.QuestionnaireDTO;
import com.zfc.eldercare.core.service.AssessmentService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.QuestionAdminVO;
import com.zfc.eldercare.core.vo.QuestionnaireAdminVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端评测管理接口（/api/admin/assessment，详细设计文档 5.3 管理端职责 / 7.2）。
 */
@RestController
@RequestMapping("/api/admin/assessment")
@RequiredArgsConstructor
public class AdminAssessmentController {

    private final AssessmentService assessmentService;

    // ========== 问卷 CRUD ==========

    /** 创建问卷（默认草稿） */
    @PostMapping("/questionnaire")
    public Result<Long> createQuestionnaire(@Valid @RequestBody QuestionnaireDTO dto) {
        return Result.success("创建成功", assessmentService.createQuestionnaire(dto));
    }

    /** 编辑问卷 */
    @PutMapping("/questionnaire/{id}")
    public Result<Void> updateQuestionnaire(@PathVariable Long id,
                                            @Valid @RequestBody QuestionnaireDTO dto) {
        assessmentService.updateQuestionnaire(id, dto);
        return Result.success();
    }

    /** 删除问卷（连同题目逻辑删除） */
    @DeleteMapping("/questionnaire/{id}")
    public Result<Void> deleteQuestionnaire(@PathVariable Long id) {
        assessmentService.deleteQuestionnaire(id);
        return Result.success();
    }

    /** 发布/下架（status=DRAFT/PUBLISHED） */
    @PutMapping("/questionnaire/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        assessmentService.updateQuestionnaireStatus(id, status);
        return Result.success();
    }

    /** 问卷列表（含草稿，分页） */
    @GetMapping("/questionnaires")
    public Result<PageVO<QuestionnaireAdminVO>> questionnaires(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return Result.success(assessmentService.adminQuestionnaires(page, size));
    }

    // ========== 题目管理 ==========

    /** 某问卷下全部题目 */
    @GetMapping("/questionnaire/{id}/questions")
    public Result<List<QuestionAdminVO>> questions(@PathVariable Long id) {
        return Result.success(assessmentService.questionsOf(id));
    }

    /** 批量保存题目（覆盖式） */
    @PostMapping("/questionnaire/{id}/questions")
    public Result<Void> saveQuestions(@PathVariable Long id,
                                      @Valid @RequestBody List<QuestionDTO> questions) {
        assessmentService.saveQuestions(id, questions);
        return Result.success();
    }

    /** 编辑单题 */
    @PutMapping("/question/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id,
                                       @Valid @RequestBody QuestionDTO dto) {
        assessmentService.updateQuestion(id, dto);
        return Result.success();
    }

    /** 删除单题 */
    @DeleteMapping("/question/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        assessmentService.deleteQuestion(id);
        return Result.success();
    }
}
