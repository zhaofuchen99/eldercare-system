package com.zfc.eldercare.core.vo;

/**
 * 仪表盘统计 VO（管理端，文档 5.9）。
 */
public record DashboardVO(
        long totalMembers,
        long todayNewMembers,
        long todayAppointments,
        long todayActivityRegistrations,
        long pendingAppointments
) {
}
