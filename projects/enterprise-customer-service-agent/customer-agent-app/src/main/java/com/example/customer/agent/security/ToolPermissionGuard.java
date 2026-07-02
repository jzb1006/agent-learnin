package com.example.customer.agent.security;

import com.example.customer.domain.approval.ApprovalAction;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 工具权限守卫。
 * <p>
 * Java 层在执行工具前做最终权限判断，Prompt 或模型输出不能绕过该守卫。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
@Component
public class ToolPermissionGuard {

    /**
     * 要求工具可直接执行。
     *
     * @param toolName 工具名称
     * @param riskLevel 工具风险级别
     * @param action 对应审批动作
     * @return 权限决策
     */
    public ToolPermissionDecision requireAllowed(
            String toolName,
            ToolRiskLevel riskLevel,
            ApprovalAction action) {
        var normalizedToolName = DomainText.requireNonBlank(toolName, "tool name");
        Objects.requireNonNull(riskLevel, "tool risk level must not be null");
        Objects.requireNonNull(action, "approval action must not be null");

        if (riskLevel.requiresApproval()) {
            throw new ToolPermissionDeniedException(
                    "tool " + normalizedToolName + " requires approval before execution");
        }
        return new ToolPermissionDecision(normalizedToolName, true, false);
    }
}
