package com.example.customer.agent.approval;

import com.example.customer.agent.observability.RequestTraceContext;
import com.example.customer.agent.security.RedactionService;
import com.example.customer.domain.approval.ApprovalAction;
import com.example.customer.domain.approval.ApprovalRequest;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 审批请求服务。
 * <p>
 * 当前阶段只创建待审批记录并返回脱敏 trace，不执行真实退款、取消或改签。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final RedactionService redactionService;

    /**
     * 创建待人工审批请求。
     *
     * @param tenantId 租户 ID
     * @param request 创建请求
     * @return 审批响应
     */
    public ApprovalResponse createPending(String tenantId, ApprovalCreateRequest request) {
        var normalizedTenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        var normalizedOrderId = DomainText.requireNonBlank(request.orderId(), "order id");
        var riskLevel = riskLevelFor(request.action());
        if (!riskLevel.requiresApproval()) {
            throw new IllegalArgumentException("approval action must be high risk: " + request.action());
        }
        var redactedReason = redactionService.redact(request.reason());
        var approvalRequest = ApprovalRequest.pending(
                "approval-" + UUID.randomUUID(),
                normalizedTenantId,
                normalizedOrderId,
                request.action(),
                riskLevel,
                redactedReason,
                Instant.now());
        var redactedTrace = redactionService.redact("""
                traceId=%s tenantId=%s orderId=%s action=%s reason=%s executed=false
                """.formatted(
                RequestTraceContext.currentTraceIdOr(""),
                normalizedTenantId,
                normalizedOrderId,
                request.action().name(),
                request.reason()).strip());
        return ApprovalResponse.from(approvalRequest, redactedReason, redactedTrace);
    }

    private ToolRiskLevel riskLevelFor(ApprovalAction action) {
        return switch (action) {
            case REFUND_ORDER, CANCEL_ORDER, RESCHEDULE_ORDER -> ToolRiskLevel.HIGH_RISK;
            case ORDER_LOOKUP -> ToolRiskLevel.READ_ONLY;
        };
    }
}
