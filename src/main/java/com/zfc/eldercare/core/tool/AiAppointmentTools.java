package com.zfc.eldercare.core.tool;

import com.zfc.eldercare.core.dto.AppointmentCreateDTO;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.vo.PackageVO;
import com.zfc.eldercare.core.vo.SlotVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话工具：体检预约（工具调用，文档 5.4 AI 对话增强）。
 *
 * 设计要点：
 * 1. 非 Spring Bean——按次调用实例化并绑定当前会员 userId（SSE 流式在响应式线程执行，
 *    不能依赖 ThreadLocal / RequestScope），实例直接传给 {@code ChatClient.tools(...)}。
 * 2. 工具内捕获所有异常并转成可读文案返回，避免预约失败导致整轮对话调用中断。
 * 3. 安全：bookAppointment 会按套餐价扣除会员积分，只有在用户明确确认后才可调用
 *   （该约束同时在 system 提示词中强调）。
 */
public class AiAppointmentTools {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 未指定日期时，聚合未来 7 天的可约时段供 AI 转述 */
    private static final int FUTURE_DAY_SCAN = 7;

    private final AppointmentService appointmentService;
    private final Long userId;

    public AiAppointmentTools(AppointmentService appointmentService, Long userId) {
        this.appointmentService = appointmentService;
        this.userId = userId;
    }

    /** 查询当前可预约的体检套餐（含套餐ID、名称、价格[积分]、适合人群、说明、包含项目） */
    @Tool(name = "queryPackages",
            description = "查询当前可预约的体检套餐列表（套餐ID、名称、价格[积分]、适合人群、说明），供用户选择。调用后向用户展示套餐并询问选择哪一个。")
    public String queryPackages() {
        try {
            List<PackageVO> list = appointmentService.packages();
            if (list == null || list.isEmpty()) {
                return "当前没有可预约的体检套餐。";
            }
            StringBuilder sb = new StringBuilder("可预约的体检套餐：\n");
            for (PackageVO p : list) {
                sb.append("- 套餐ID ").append(p.id())
                  .append("，名称「").append(p.name()).append("」")
                  .append("，价格 ").append(p.price()).append(" 积分")
                  .append("，适合人群：").append(StringUtils.hasText(p.suitablePeople()) ? p.suitablePeople() : "不限");
                if (p.items() != null && !p.items().isEmpty()) {
                    sb.append("，包含项目：").append(String.join("、", p.items()));
                }
                if (p.description() != null && !p.description().isBlank()) {
                    sb.append("\n  说明：").append(p.description());
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return "查询套餐失败：" + e.getMessage();
        }
    }

    /** 查询指定套餐的可预约时段；未指定日期时聚合未来 7 天 */
    @Tool(name = "querySlots",
            description = "查询指定体检套餐的可预约时段（时段ID、时间、剩余名额）。date 可选，缺省返回未来 7 天；指定则只返回该日期。调用后向用户展示时段并询问选择，预约前应告知将扣除的积分。")
    public String querySlots(
            @ToolParam(description = "体检套餐ID（先通过 queryPackages 获取）", required = true) Long packageId,
            @ToolParam(description = "预约日期，格式 yyyy-MM-dd，可选，缺省聚合未来 7 天", required = false) String date) {
        if (packageId == null) {
            return "缺少套餐ID，请先通过 queryPackages 查询套餐并确认用户选择。";
        }
        try {
            List<SlotVO> list;
            if (StringUtils.hasText(date)) {
                LocalDate day = LocalDate.parse(date, DATE_FMT);
                list = safeSlots(packageId, day);
            } else {
                list = new ArrayList<>();
                for (int i = 0; i < FUTURE_DAY_SCAN; i++) {
                    list.addAll(safeSlots(packageId, LocalDate.now().plusDays(i)));
                }
            }
            if (list.isEmpty()) {
                return "套餐ID " + packageId + " 近期没有可预约时段，可换个套餐或日期再试。";
            }
            StringBuilder sb = new StringBuilder("套餐ID " + packageId + " 的可预约时段：\n");
            for (SlotVO s : list) {
                sb.append("- 时段ID ").append(s.id())
                  .append("，").append(s.appointDate()).append(" ").append(s.timeRange())
                  .append("，剩余名额 ").append(s.maxCount() - s.currentCount()).append("/").append(s.maxCount())
                  .append("\n");
            }
            return sb.toString();
        } catch (DateTimeParseException e) {
            return "日期格式不正确，请使用 yyyy-MM-dd，例如 2026-08-25。";
        } catch (RuntimeException e) {
            return "查询时段失败：" + e.getMessage();
        }
    }

    /** 提交体检预约（扣积分 + 并发名额占用）。仅用户明确确认后调用，confirm 必须为 true。 */
    @Tool(name = "bookAppointment",
            description = "为当前会员提交体检预约并扣除对应积分。必须先通过 querySlots 告知用户所选时段与将扣除的积分，并在用户明确同意（如“好的/确认/可以”）后才调用，且 confirm 必须传 true。")
    public String bookAppointment(
            @ToolParam(description = "要预约的时段ID（先通过 querySlots 获取）", required = true) Long slotId,
            @ToolParam(description = "用户是否已明确确认预约：只有用户明确同意才传 true，否则必须传 false", required = true) Boolean confirm) {
        if (slotId == null) {
            return "缺少时段ID，请先通过 querySlots 查询时段并确认用户选择。";
        }
        if (!Boolean.TRUE.equals(confirm)) {
            return "用户尚未确认预约，请先向用户说明所选时段与将扣除的积分并征得明确同意，确认后再调用本工具。";
        }
        try {
            Long appointmentId = appointmentService.create(userId, new AppointmentCreateDTO(slotId));
            return "预约成功！预约ID：" + appointmentId + "，已扣除相应积分，状态为待确认，可在「我的预约」中查看。";
        } catch (BusinessException e) {
            return "预约失败：" + e.getMessage();
        } catch (RuntimeException e) {
            return "预约失败，请稍后重试：" + e.getMessage();
        }
    }

    private List<SlotVO> safeSlots(Long packageId, LocalDate day) {
        List<SlotVO> list = appointmentService.availableSlots(packageId, day);
        return list == null ? List.of() : list;
    }
}
