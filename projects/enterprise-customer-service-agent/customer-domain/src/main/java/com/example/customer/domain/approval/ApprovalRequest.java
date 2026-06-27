package com.example.customer.domain.approval;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Instant;
import java.util.Objects;

/**
 * 高风险动作审批请求。
 * <p>
 * 该模型只表达审批边界，不执行退款、取消或改签等真实写操作。
 *
 * @param id 审批请求唯一标识
 * @param tenantId 归属租户标识
 * @param orderId 关联订单标识
 * @param action 审批动作
 * @param riskLevel 工具风险级别
 * @param reason 审批原因
 * @param status 审批状态
 * @param requestedAt 申请时间
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record ApprovalRequest(
        String id,
        String tenantId,
        String orderId,
        ApprovalAction action,
        ToolRiskLevel riskLevel,
        String reason,
        ApprovalStatus status,
        Instant requestedAt) {

    /**
     * 创建待审批请求。
     *
     * @param id 审批请求唯一标识
     * @param tenantId 归属租户标识
     * @param orderId 关联订单标识
     * @param action 审批动作
     * @param riskLevel 工具风险级别
     * @param reason 审批原因
     * @param requestedAt 申请时间
     * @return 待审批请求
     */
    public static ApprovalRequest pending(
            String id,
            String tenantId,
            String orderId,
            ApprovalAction action,
            ToolRiskLevel riskLevel,
            String reason,
            Instant requestedAt) {
        return new ApprovalRequest(
                id,
                tenantId,
                orderId,
                action,
                riskLevel,
                reason,
                ApprovalStatus.PENDING,
                requestedAt);
    }

    public ApprovalRequest {
        id = DomainText.requireNonBlank(id, "approval id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        orderId = DomainText.requireNonBlank(orderId, "order id");
        Objects.requireNonNull(action, "approval action must not be null");
        Objects.requireNonNull(riskLevel, "tool risk level must not be null");
        reason = DomainText.requireNonBlank(reason, "approval reason");
        Objects.requireNonNull(status, "approval status must not be null");
        Objects.requireNonNull(requestedAt, "approval requested at must not be null");
        if (!riskLevel.requiresApproval()) {
            throw new IllegalArgumentException("approval requires a high risk tool");
        }
    }

    /**
     * 判断审批请求是否仍需要人工决策。
     *
     * @return 待审批状态返回 true
     */
    public boolean requiresHumanDecision() {
        return status == ApprovalStatus.PENDING;
    }
}
