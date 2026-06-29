package com.example.customer.domain.tool;

import java.util.Objects;

/**
 * 工具权限策略。
 * <p>
 * 统一表达工具是否默认允许执行、是否需要显式启用以及是否必须人工审批。
 *
 * @param executionAllowed 默认是否允许执行
 * @param explicitEnableRequired 是否需要显式启用
 * @param approvalRequired 是否必须人工审批
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public record ToolPermission(
        boolean executionAllowed,
        boolean explicitEnableRequired,
        boolean approvalRequired) {

    /**
     * 创建只读工具默认允许策略。
     *
     * @return 只读允许策略
     */
    public static ToolPermission allowReadOnly() {
        return new ToolPermission(true, false, false);
    }

    /**
     * 创建低风险写工具默认关闭策略。
     *
     * @return 低风险写默认关闭策略
     */
    public static ToolPermission disabledLowRiskWrite() {
        return new ToolPermission(false, true, false);
    }

    /**
     * 创建高风险工具审批策略。
     *
     * @return 高风险审批策略
     */
    public static ToolPermission requireApproval() {
        return new ToolPermission(false, true, true);
    }

    /**
     * 按工具风险级别推导默认权限策略。
     *
     * @param riskLevel 工具风险级别
     * @return 默认权限策略
     */
    public static ToolPermission defaultFor(ToolRiskLevel riskLevel) {
        Objects.requireNonNull(riskLevel, "tool risk level must not be null");
        return switch (riskLevel) {
            case READ_ONLY -> allowReadOnly();
            case LOW_RISK_WRITE -> disabledLowRiskWrite();
            case HIGH_RISK -> requireApproval();
        };
    }

    public ToolPermission {
        if (approvalRequired && executionAllowed) {
            throw new IllegalArgumentException("approval required tool must not be execution allowed by default");
        }
        if (approvalRequired && !explicitEnableRequired) {
            throw new IllegalArgumentException("approval required tool must require explicit enable");
        }
    }
}
