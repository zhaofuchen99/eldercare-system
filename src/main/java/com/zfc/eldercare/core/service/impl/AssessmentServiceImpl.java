package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.AssessmentSubmitDTO;
import com.zfc.eldercare.core.dto.QuestionDTO;
import com.zfc.eldercare.core.dto.QuestionnaireDTO;
import com.zfc.eldercare.core.entity.AssessmentResult;
import com.zfc.eldercare.core.entity.Question;
import com.zfc.eldercare.core.entity.Questionnaire;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.AssessmentResultMapper;
import com.zfc.eldercare.core.mapper.QuestionMapper;
import com.zfc.eldercare.core.mapper.QuestionnaireMapper;
import com.zfc.eldercare.core.service.AssessmentAiScorer;
import com.zfc.eldercare.core.service.AssessmentService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.vo.AssessmentResultDetailVO;
import com.zfc.eldercare.core.vo.AssessmentResultListVO;
import com.zfc.eldercare.core.vo.GradeRuleVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireDetailVO;
import com.zfc.eldercare.core.vo.MemberQuestionnaireVO;
import com.zfc.eldercare.core.vo.MemberQuestionVO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.QuestionAdminVO;
import com.zfc.eldercare.core.vo.QuestionOptionVO;
import com.zfc.eldercare.core.vo.QuestionnaireAdminVO;
import com.zfc.eldercare.core.vo.ResultItemVO;
import com.zfc.eldercare.core.vo.SnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康评测服务实现（详细设计文档 5.3）。
 * 评分机制：规则分仅计分题按选项分值加总换算百分制；AI 评分当前回退为规则分（Mock），
 * 接入 DeepSeek 后替换 AssessmentAiScorer 实现即可。
 */
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DRAFT = "DRAFT";

    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionMapper questionMapper;
    private final AssessmentResultMapper assessmentResultMapper;
    private final AssessmentAiScorer aiScorer;
    private final PointsService pointsService;
    private final ObjectMapper objectMapper;

    // ========== 会员端 ==========

    @Override
    public List<MemberQuestionnaireVO> publishedQuestionnaires() {
        return questionnaireMapper.selectPublished().stream()
                .map(q -> new MemberQuestionnaireVO(q.getId(), q.getTitle(), q.getDescription()))
                .toList();
    }

    @Override
    public MemberQuestionnaireDetailVO questionnaireDetail(Long questionnaireId) {
        Questionnaire q = getQuestionnaire(questionnaireId);
        if (!STATUS_PUBLISHED.equals(q.getStatus())) {
            throw new BusinessException(403, "问卷未发布，暂不可答题");
        }
        List<MemberQuestionVO> questions = questionMapper.selectByQuestionnaireId(questionnaireId).stream()
                .map(question -> new MemberQuestionVO(question.getId(), question.getContent(), question.getType(),
                        parseOptions(question.getOptions()).stream().map(QuestionOptionVO::text).toList()))
                .toList();
        return new MemberQuestionnaireDetailVO(q.getId(), q.getTitle(), q.getDescription(), questions);
    }

    @Override
    @Transactional
    public Long submit(Long userId, AssessmentSubmitDTO dto) {
        Questionnaire q = getQuestionnaire(dto.questionnaireId());
        if (!STATUS_PUBLISHED.equals(q.getStatus())) {
            throw new BusinessException(409, "问卷未发布，无法提交");
        }
        List<Question> questions = questionMapper.selectByQuestionnaireId(q.getId());
        if (questions.isEmpty()) {
            throw new BusinessException("该问卷暂无可答题目的");
        }

        Map<Long, AssessmentSubmitDTO.AnswerItemDTO> answerMap = dto.items().stream()
                .collect(Collectors.toMap(AssessmentSubmitDTO.AnswerItemDTO::qid, a -> a, (a, b) -> a));

        // 1) 逐题校验答案、计算计分题得分、组装答案快照与 AI 上下文
        List<SnapshotVO.SnapshotItemVO> snapshotItems = new ArrayList<>();
        int sumScored = 0;
        StringBuilder prompt = new StringBuilder("问卷：").append(q.getTitle()).append("\n");

        for (Question question : questions) {
            AssessmentSubmitDTO.AnswerItemDTO answer = answerMap.get(question.getId());
            if (answer == null || answer.value() == null) {
                throw new BusinessException("请完成题目：" + question.getContent());
            }
            List<QuestionOptionVO> options = parseOptions(question.getOptions());
            boolean scored = "SCORED".equals(question.getScoreMode());

            Integer score = null;
            Object snapshotValue;
            switch (question.getType()) {
                case "SINGLE" -> {
                    Integer idx = toIndex(answer.value(), options.size());
                    if (idx == null) {
                        throw new BusinessException("请完成题目：" + question.getContent());
                    }
                    snapshotValue = idx;
                    if (scored && options.get(idx).score() != null) {
                        score = options.get(idx).score();
                    }
                }
                case "MULTIPLE" -> {
                    List<Integer> idxs = toIndexList(answer.value(), options.size());
                    if (idxs == null) {
                        throw new BusinessException("请完成题目：" + question.getContent());
                    }
                    snapshotValue = idxs;
                    if (scored) {
                        int s = 0;
                        for (Integer i : idxs) {
                            if (options.get(i).score() != null) {
                                s += options.get(i).score();
                            }
                        }
                        // 多选得分上限为该题 max_score（文档 5.3）
                        if (question.getMaxScore() != null) {
                            s = Math.min(s, question.getMaxScore());
                        }
                        score = s;
                    }
                }
                case "TEXT" -> {
                    if (!(answer.value() instanceof String s) || s.isBlank()) {
                        throw new BusinessException("请完成题目：" + question.getContent());
                    }
                    snapshotValue = s;
                }
                default -> throw new BusinessException("题目类型不合法：" + question.getType());
            }

            if (scored && score != null) {
                sumScored += score;
            }
            snapshotItems.add(new SnapshotVO.SnapshotItemVO(question.getId(), question.getType(),
                    snapshotValue, scored ? score : null, question.getMaxScore()));

            // 2) AI 上下文：题目 + 选项（text/meaning/score）+ 用户答案
            prompt.append("\n【").append(question.getContent()).append("】（类型：")
                  .append(question.getType()).append("）\n选项：");
            for (QuestionOptionVO opt : options) {
                prompt.append(opt.text());
                if (opt.meaning() != null) {
                    prompt.append("（").append(opt.meaning()).append("）");
                }
                if (opt.score() != null) {
                    prompt.append("[分值").append(opt.score()).append("]");
                }
                prompt.append("；");
            }
            prompt.append("\n用户答案：").append(formatValue(question.getType(), snapshotValue));
            if (score != null) {
                prompt.append("（该题得分 ").append(score).append("）");
            }
        }

        // 3) 规则分：rule_score = ROUND(Σ计分题得分 / total_score × 100)
        int ruleScore = 0;
        if (q.getTotalScore() != null && q.getTotalScore() > 0) {
            ruleScore = Math.round(sumScored * 100f / q.getTotalScore());
        }

        // 4) AI 评分（Mock：ai_score = 规则分）与等级判定
        AssessmentAiScorer.AiScoreResult ai = aiScorer.score(prompt.toString(), ruleScore);
        String grade = matchGrade(ai.aiScore(), q.getGradeRules());

        // 5) 保存结果并赠送积分
        SnapshotVO snapshot = new SnapshotVO(1, snapshotItems);
        AssessmentResult result = new AssessmentResult();
        result.setUserId(userId);
        result.setQuestionnaireId(q.getId());
        result.setAnswers(writeJson(snapshot));
        result.setRuleScore(ruleScore);
        result.setAiScore(ai.aiScore());
        result.setAiSuggestion(ai.suggestion());
        assessmentResultMapper.insert(result);

        pointsService.assessmentBonus(userId);

        return result.getId();
    }

    @Override
    public PageVO<AssessmentResultListVO> history(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<AssessmentResult> list = assessmentResultMapper.selectPageByUserId(userId);
        PageInfo<AssessmentResult> pageInfo = new PageInfo<>(list);

        List<Long> ids = list.stream().map(AssessmentResult::getQuestionnaireId).distinct().toList();
        Map<Long, String> titles = ids.isEmpty() ? Map.of() : questionnaireMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Questionnaire::getId, Questionnaire::getTitle));

        List<AssessmentResultListVO> voList = list.stream()
                .map(r -> new AssessmentResultListVO(r.getId(), r.getQuestionnaireId(),
                        titles.getOrDefault(r.getQuestionnaireId(), "问卷已删除"),
                        r.getRuleScore(), r.getAiScore(), r.getCreateTime()))
                .toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public AssessmentResultDetailVO resultDetail(Long userId, Long resultId) {
        AssessmentResult result = assessmentResultMapper.selectById(resultId);
        if (result == null) {
            throw new BusinessException(404, "评测记录不存在");
        }
        if (!result.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人的评测记录");
        }
        Questionnaire q = questionnaireMapper.selectById(result.getQuestionnaireId());
        Map<Long, Question> questionMap = questionMapper.selectByQuestionnaireId(result.getQuestionnaireId()).stream()
                .collect(Collectors.toMap(Question::getId, x -> x));

        SnapshotVO snapshot = readSnapshot(result.getAnswers());
        List<ResultItemVO> items = snapshot.items().stream()
                .map(item -> buildResultItem(item, questionMap))
                .filter(java.util.Objects::nonNull)
                .toList();
        String grade = q != null ? matchGrade(result.getAiScore(), q.getGradeRules()) : null;

        return new AssessmentResultDetailVO(result.getId(), result.getQuestionnaireId(),
                q != null ? q.getTitle() : "问卷已删除",
                result.getRuleScore(), result.getAiScore(), result.getAiSuggestion(),
                grade, result.getCreateTime(), items);
    }

    // ========== 管理端 ==========

    @Override
    public Long createQuestionnaire(QuestionnaireDTO dto) {
        Questionnaire q = new Questionnaire();
        applyQuestionnaire(q, dto);
        q.setStatus(STATUS_DRAFT);
        questionnaireMapper.insert(q);
        return q.getId();
    }

    @Override
    @Transactional
    public void updateQuestionnaire(Long id, QuestionnaireDTO dto) {
        getQuestionnaire(id);
        Questionnaire q = new Questionnaire();
        q.setId(id);
        applyQuestionnaire(q, dto);
        questionnaireMapper.update(q);
    }

    @Override
    @Transactional
    public void deleteQuestionnaire(Long id) {
        getQuestionnaire(id);
        questionnaireMapper.delete(id);
        questionMapper.deleteByQuestionnaireId(id);
    }

    @Override
    public void updateQuestionnaireStatus(Long id, String status) {
        getQuestionnaire(id);
        if (!STATUS_PUBLISHED.equals(status) && !STATUS_DRAFT.equals(status)) {
            throw new BusinessException("状态不合法，仅支持 DRAFT/PUBLISHED");
        }
        questionnaireMapper.updateStatus(id, status);
    }

    @Override
    public PageVO<QuestionnaireAdminVO> adminQuestionnaires(int page, int size) {
        PageHelper.startPage(page, size);
        List<Questionnaire> list = questionnaireMapper.selectPage();
        PageInfo<Questionnaire> pageInfo = new PageInfo<>(list);
        List<QuestionnaireAdminVO> voList = list.stream()
                .map(q -> new QuestionnaireAdminVO(q.getId(), q.getTitle(), q.getDescription(), q.getStatus(),
                        q.getTotalScore(), q.getPassScore(), readGradeRules(q.getGradeRules()), q.getCreateTime()))
                .toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public List<QuestionAdminVO> questionsOf(Long questionnaireId) {
        getQuestionnaire(questionnaireId);
        return questionMapper.selectByQuestionnaireId(questionnaireId).stream()
                .map(question -> new QuestionAdminVO(question.getId(), question.getContent(), question.getType(),
                        question.getScoreMode(), parseOptions(question.getOptions()),
                        question.getMaxScore(), question.getSortOrder()))
                .toList();
    }

    @Override
    @Transactional
    public void saveQuestions(Long questionnaireId, List<QuestionDTO> questions) {
        getQuestionnaire(questionnaireId);
        questionMapper.deleteByQuestionnaireId(questionnaireId);
        for (QuestionDTO dto : questions) {
            insertQuestion(questionnaireId, dto);
        }
    }

    @Override
    public void updateQuestion(Long id, QuestionDTO dto) {
        if (questionMapper.selectById(id) == null) {
            throw new BusinessException(404, "题目不存在");
        }
        Question question = toQuestion(dto);
        question.setId(id);
        questionMapper.update(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        if (questionMapper.selectById(id) == null) {
            throw new BusinessException(404, "题目不存在");
        }
        questionMapper.delete(id);
    }

    // ========== 私有辅助 ==========

    private void insertQuestion(Long questionnaireId, QuestionDTO dto) {
        if ("TEXT".equals(dto.type())) {
            if (dto.options() != null && !dto.options().isEmpty()) {
                throw new BusinessException("文本题不能配置选项");
            }
        } else if (dto.options() == null || dto.options().isEmpty()) {
            throw new BusinessException("题目「" + dto.content() + "」需配置至少一个选项");
        }
        Question question = toQuestion(dto);
        question.setQuestionnaireId(questionnaireId);
        questionMapper.insert(question);
    }

    private Question toQuestion(QuestionDTO dto) {
        Question question = new Question();
        question.setContent(dto.content());
        question.setType(dto.type());
        question.setScoreMode(dto.scoreMode());
        question.setMaxScore(dto.maxScore());
        question.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());
        question.setOptions(dto.options() == null || dto.options().isEmpty()
                ? null : writeJson(dto.options()));
        return question;
    }

    private void applyQuestionnaire(Questionnaire q, QuestionnaireDTO dto) {
        q.setTitle(dto.title());
        q.setDescription(dto.description());
        q.setTotalScore(dto.totalScore());
        q.setPassScore(dto.passScore());
        q.setGradeRules(dto.gradeRules() == null ? null : writeJson(dto.gradeRules()));
    }

    private Questionnaire getQuestionnaire(Long id) {
        Questionnaire q = questionnaireMapper.selectById(id);
        if (q == null) {
            throw new BusinessException(404, "问卷不存在");
        }
        return q;
    }

    /** 单选答案转选项下标（越界返回 null） */
    private Integer toIndex(Object value, int optionCount) {
        if (value instanceof Number n) {
            int idx = n.intValue();
            if (idx >= 0 && idx < optionCount) {
                return idx;
            }
        }
        return null;
    }

    /** 多选答案转下标数组（去重、越界剔除；空结果返回 null） */
    private List<Integer> toIndexList(Object value, int optionCount) {
        if (value instanceof List<?> list) {
            List<Integer> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Number n) {
                    int idx = n.intValue();
                    if (idx >= 0 && idx < optionCount && !result.contains(idx)) {
                        result.add(idx);
                    }
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    private String formatValue(String type, Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(value);
    }

    /** 按等级规则匹配 ai_score 得出等级（min 越大优先级越高） */
    private String matchGrade(Integer aiScore, String gradeRulesJson) {
        if (aiScore == null) {
            return null;
        }
        List<GradeRuleVO> rules = readGradeRules(gradeRulesJson);
        return rules.stream()
                .filter(r -> r.min() != null)
                .sorted(Comparator.comparingInt(GradeRuleVO::min).reversed())
                .filter(r -> aiScore >= r.min())
                .map(GradeRuleVO::label)
                .findFirst()
                .orElse(null);
    }

    /** 评测报告单题：把快照还原为选项文案/语义/得分 */
    private ResultItemVO buildResultItem(SnapshotVO.SnapshotItemVO item, Map<Long, Question> questionMap) {
        Question question = questionMap.get(item.qid());
        if (question == null) {
            return null;
        }
        String answerText;
        String meaning = null;
        if ("TEXT".equals(item.type())) {
            answerText = String.valueOf(item.value());
        } else {
            List<QuestionOptionVO> options = parseOptions(question.getOptions());
            if (item.value() instanceof List<?> list) {
                List<String> texts = new ArrayList<>();
                List<String> meanings = new ArrayList<>();
                for (Object o : list) {
                    int idx = ((Number) o).intValue();
                    if (idx >= 0 && idx < options.size()) {
                        texts.add(options.get(idx).text());
                        if (options.get(idx).meaning() != null) {
                            meanings.add(options.get(idx).meaning());
                        }
                    }
                }
                answerText = String.join("、", texts);
                meaning = String.join("、", meanings);
            } else if (item.value() instanceof Number n) {
                int idx = n.intValue();
                if (idx >= 0 && idx < options.size()) {
                    QuestionOptionVO opt = options.get(idx);
                    answerText = opt.text();
                    meaning = opt.meaning();
                } else {
                    answerText = "";
                }
            } else {
                answerText = "";
            }
        }
        return new ResultItemVO(item.qid(), question.getContent(), item.type(), answerText, meaning, item.score());
    }

    // ========== JSON 工具 ==========

    private List<QuestionOptionVO> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<QuestionOptionVO>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("题目选项数据格式错误");
        }
    }

    private List<GradeRuleVO> readGradeRules(String gradeRulesJson) {
        if (!StringUtils.hasText(gradeRulesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(gradeRulesJson, new TypeReference<List<GradeRuleVO>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("问卷等级规则数据格式错误");
        }
    }

    private SnapshotVO readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, SnapshotVO.class);
        } catch (Exception e) {
            throw new BusinessException("评测数据格式错误");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("数据序列化失败");
        }
    }
}
