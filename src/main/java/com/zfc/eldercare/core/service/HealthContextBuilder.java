package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.entity.Appointment;
import com.zfc.eldercare.core.entity.AppointmentPackage;
import com.zfc.eldercare.core.entity.AssessmentResult;
import com.zfc.eldercare.core.entity.HealthRecord;
import com.zfc.eldercare.core.entity.Questionnaire;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.mapper.AppointmentMapper;
import com.zfc.eldercare.core.mapper.AppointmentPackageMapper;
import com.zfc.eldercare.core.mapper.AssessmentResultMapper;
import com.zfc.eldercare.core.mapper.HealthRecordMapper;
import com.zfc.eldercare.core.mapper.QuestionnaireMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.vo.GradeRuleVO;
import com.zfc.eldercare.core.vo.HealthTrendVO;
import com.zfc.eldercare.core.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户健康上下文构建器（AI 模块 - 健康数据增强）。
 * 根据 userId 查询用户档案/最新健康记录/近6月趋势/最近评测/体检记录，生成注入大模型 Prompt
 * 的个性化健康上下文（纯中文分段文本）。
 *
 * <p>纯函数组件：与聊天链路解耦，仅依赖各业务 Mapper/Service，可被 AI 对话及其他模块复用。
 * 用户不存在或全无健康数据时返回空串，调用方据此决定是否注入。
 */
@Component
@RequiredArgsConstructor
public class HealthContextBuilder {

    /** 指标编码 → 中文 */
    private static final Map<String, String> INDICATOR_TEXT = Map.of(
            "SYSTOLIC", "收缩压",
            "DIASTOLIC", "舒张压",
            "BLOOD_SUGAR", "空腹血糖",
            "HEART_RATE", "心率",
            "BMI", "BMI");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserMapper userMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final HealthRecordService healthRecordService;
    private final AssessmentResultMapper assessmentResultMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentPackageMapper appointmentPackageMapper;
    private final ObjectMapper objectMapper;

    /** 生成用户健康上下文；无数据段自动省略，全无返回空串 */
    public String build(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        append(sb, profile(user));
        append(sb, latestRecord(userId));
        append(sb, trend(userId));
        append(sb, assessment(userId));
        append(sb, appointment(userId));
        return sb.toString().trim();
    }

    private void append(StringBuilder sb, String section) {
        if (StringUtils.hasText(section)) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(section);
        }
    }

    /** 用户基本信息（姓名脱敏） */
    private String profile(User user) {
        List<String> parts = new ArrayList<>();
        parts.add(user.getBirthDate() == null ? "年龄未知" : "年龄" + calcAge(user.getBirthDate()) + "岁");
        String gender = genderText(user.getGender());
        if (StringUtils.hasText(gender)) {
            parts.add("性别" + gender);
        }
        if (user.getHeight() != null) {
            parts.add("身高" + user.getHeight() + "cm");
        }
        if (StringUtils.hasText(user.getMemberLevel())) {
            parts.add("会员等级" + levelText(user.getMemberLevel()));
        }
        return "【用户档案】" + maskName(user.getRealName()) + "，" + String.join("，", parts);
    }

    /** 最近一次健康记录指标 */
    private String latestRecord(Long userId) {
        List<HealthRecord> records = healthRecordMapper.selectByUserId(userId);
        if (records.isEmpty()) {
            return "";
        }
        HealthRecord r = records.getFirst();
        List<String> parts = new ArrayList<>();
        if (r.getSystolic() != null || r.getDiastolic() != null) {
            parts.add("血压" + (r.getSystolic() == null ? "?" : r.getSystolic())
                    + "/" + (r.getDiastolic() == null ? "?" : r.getDiastolic()) + "mmHg");
        }
        if (r.getBloodSugar() != null) {
            parts.add("空腹血糖" + r.getBloodSugar() + "mmol/L");
        }
        if (r.getHeartRate() != null) {
            parts.add("心率" + r.getHeartRate() + "次/分");
        }
        if (r.getWeight() != null) {
            parts.add("体重" + r.getWeight() + "kg");
        }
        if (r.getBmi() != null) {
            parts.add("BMI" + r.getBmi());
        }
        if (parts.isEmpty()) {
            return "";
        }
        String date = r.getRecordedTime() == null ? "" : "（记录于" + DATE_FMT.format(r.getRecordedTime().toLocalDate()) + "）";
        return "【最新健康数据】" + String.join("，", parts) + date;
    }

    /** 近 6 月趋势：每个指标取最近 3 个月（均值/峰值） */
    private String trend(Long userId) {
        HealthTrendVO vo = healthRecordService.trend(userId, null);
        Map<String, List<TrendPointVO>> data = vo == null ? Map.of() : vo.data();
        if (data.isEmpty()) {
            return "";
        }
        List<String> sections = new ArrayList<>();
        for (Map.Entry<String, List<TrendPointVO>> e : data.entrySet()) {
            List<TrendPointVO> points = e.getValue();
            if (points == null || points.isEmpty()) {
                continue;
            }
            List<TrendPointVO> recent = points.size() > 3 ? points.subList(points.size() - 3, points.size()) : points;
            String summary = recent.stream()
                    .map(p -> p.month() + "均值" + p.avg() + "(峰值" + p.max() + ")")
                    .collect(Collectors.joining("，"));
            sections.add(INDICATOR_TEXT.getOrDefault(e.getKey(), e.getKey()) + " " + summary);
        }
        if (sections.isEmpty()) {
            return "";
        }
        return "【近6月趋势】" + String.join("；", sections);
    }

    /** 最近一次健康评测：问卷标题 + AI 评分 + 等级 + 建议摘要 */
    private String assessment(Long userId) {
        List<AssessmentResult> results = assessmentResultMapper.selectPageByUserId(userId);
        if (results.isEmpty()) {
            return "";
        }
        AssessmentResult r = results.getFirst();
        Questionnaire q = questionnaireMapper.selectById(r.getQuestionnaireId());
        String title = q == null ? "健康评测" : q.getTitle();
        String grade = matchGrade(r.getAiScore(), q == null ? null : q.getGradeRules());
        List<String> parts = new ArrayList<>();
        parts.add("问卷" + title);
        parts.add("AI评分" + (r.getAiScore() == null ? "暂无" : r.getAiScore() + "分"));
        if (StringUtils.hasText(grade)) {
            parts.add("等级" + grade);
        }
        String s = "【最近健康评测】" + String.join("，", parts);
        if (StringUtils.hasText(r.getAiSuggestion())) {
            String suggestion = r.getAiSuggestion();
            if (suggestion.length() > 80) {
                suggestion = suggestion.substring(0, 80);
            }
            s += "。AI建议摘要：" + suggestion;
        }
        return s;
    }

    /** 最近一次已完成体检（套餐名/报告状态） */
    private String appointment(Long userId) {
        List<Appointment> list = appointmentMapper.selectPageByUserId(userId);
        Appointment done = list.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .findFirst()
                .orElse(null);
        if (done == null) {
            return "";
        }
        AppointmentPackage pkg = appointmentPackageMapper.selectById(done.getPackageId());
        String pkgName = pkg == null ? "体检套餐" : pkg.getName();
        StringBuilder sb = new StringBuilder("【体检记录】最近一次已完成体检：套餐" + pkgName);
        if (StringUtils.hasText(done.getOriginalFilename())) {
            sb.append("，已上传报告");
        }
        if (done.getCreateTime() != null) {
            sb.append("（提交于").append(DATE_FMT.format(done.getCreateTime().toLocalDate())).append("）");
        }
        return sb.toString();
    }

    /** 按等级规则匹配 ai_score 得出等级（min 越大优先级越高），与 AssessmentServiceImpl 同逻辑 */
    private String matchGrade(Integer aiScore, String gradeRulesJson) {
        if (aiScore == null) {
            return null;
        }
        return readGradeRules(gradeRulesJson).stream()
                .filter(r -> r.min() != null)
                .sorted(Comparator.comparingInt(GradeRuleVO::min).reversed())
                .filter(r -> aiScore >= r.min())
                .map(GradeRuleVO::label)
                .findFirst()
                .orElse(null);
    }

    private List<GradeRuleVO> readGradeRules(String gradeRulesJson) {
        if (!StringUtils.hasText(gradeRulesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(gradeRulesJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private int calcAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /** 姓名脱敏：保留姓，其余打码 */
    private String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return "用户";
        }
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private String genderText(String v) {
        if (v == null) {
            return "";
        }
        if ("M".equals(v) || "1".equals(v) || "男".equals(v)) {
            return "男";
        }
        if ("F".equals(v) || "0".equals(v) || "女".equals(v)) {
            return "女";
        }
        return v;
    }

    private String levelText(String level) {
        return switch (level) {
            case "NORMAL" -> "普通";
            case "SILVER" -> "白银";
            case "GOLD" -> "黄金";
            case "PLATINUM" -> "铂金";
            case "DIAMOND" -> "钻石";
            default -> level;
        };
    }
}
