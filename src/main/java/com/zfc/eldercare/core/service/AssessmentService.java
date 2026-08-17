package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.AssessmentSubmitDTO;
import com.zfc.eldercare.core.dto.QuestionDTO;
import com.zfc.eldercare.core.dto.QuestionnaireDTO;
import com.zfc.eldercare.core.vo.AssessmentResultDetailVO;
import com.zfc.eldercare.core.vo.AssessmentResultListVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireDetailVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.QuestionAdminVO;
import com.zfc.eldercare.core.vo.QuestionnaireAdminVO;

import java.util.List;

/**
 * 健康评测服务（详细设计文档 5.3）。
 */
public interface AssessmentService {

    // ========== 会员端 ==========

    /** 已发布问卷列表 */
    List<MemberQuestionnaireVO> publishedQuestionnaires();

    /** 问卷详情（含题目，仅已发布可答题） */
    MemberQuestionnaireDetailVO questionnaireDetail(Long questionnaireId);

    /** 提交评测：计算规则分、AI 评分、判定等级、保存结果、赠送积分，返回结果 ID */
    Long submit(Long userId, AssessmentSubmitDTO dto);

    /** 评测历史（分页） */
    PageVO<AssessmentResultListVO> history(Long userId, int page, int size);

    /** 评测报告详情（仅本人可看） */
    AssessmentResultDetailVO resultDetail(Long userId, Long resultId);

    // ========== 管理端 ==========

    Long createQuestionnaire(QuestionnaireDTO dto);

    void updateQuestionnaire(Long id, QuestionnaireDTO dto);

    void deleteQuestionnaire(Long id);

    /** 发布/下架 */
    void updateQuestionnaireStatus(Long id, String status);

    /** 问卷列表（含草稿，分页） */
    PageVO<QuestionnaireAdminVO> adminQuestionnaires(int page, int size);

    /** 某问卷下全部题目 */
    List<QuestionAdminVO> questionsOf(Long questionnaireId);

    /** 批量保存题目（覆盖式：先逻辑删除该问卷下全部题目，再按序重建） */
    void saveQuestions(Long questionnaireId, List<QuestionDTO> questions);

    void updateQuestion(Long id, QuestionDTO dto);

    void deleteQuestion(Long id);
}
