package com.example.customer.agent.security;

/**
 * 工具权限决策。
 *
 * @param toolName 工具名称
 * @param allowed 是否允许直接执行
 * @param approvalRequired 是否必须先审批
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public record ToolPermissionDecision(
        String toolName,
        boolean allowed,
        boolean approvalRequired) {
}
