package com.example.customer.agent.approval;

import com.example.customer.domain.approval.ApprovalAction;
import com.example.customer.domain.approval.ApprovalRequest;
import com.example.customer.domain.approval.ApprovalStatus;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Instant;

/**
 * 审批调试响应。
 *
 * @param id 审批请求 ID
 * @param tenantId 租户 ID
 * @param orderId 订单号
 * @param action 审批动作
 * @param riskLevel 风险级别
 * @param status 审批状态
 * @param reason 脱敏后的审批原因
 * @param redactedTrace 脱敏后的 trace 摘要
 * @param requiresHumanDecision 是否仍需人工决策
 * @param executed 是否已执行真实高风险动作
 * @param requestedAt 创建时间
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public record ApprovalResponse(
        String id,
        String tenantId,
        String orderId,
        ApprovalAction action,
        ToolRiskLevel riskLevel,
        ApprovalStatus status,
        String reason,
        String redactedTrace,
        boolean requiresHumanDecision,
        boolean executed,
        Instant requestedAt) {

    /**
     * 从领域审批请求创建 API 响应。
     *
     * @param approvalRequest 审批请求
     * @param redactedReason 脱敏原因
     * @param redactedTrace 脱敏 trace
     * @return API 响应
     */
    public static ApprovalResponse from(
            ApprovalRequest approvalRequest,
            String redactedReason,
            String redactedTrace) {
        return new ApprovalResponse(
                approvalRequest.id(),
                approvalRequest.tenantId(),
                approvalRequest.orderId(),
                approvalRequest.action(),
                approvalRequest.riskLevel(),
                approvalRequest.status(),
                redactedReason,
                redactedTrace,
                approvalRequest.requiresHumanDecision(),
                false,
                approvalRequest.requestedAt());
    }
}
