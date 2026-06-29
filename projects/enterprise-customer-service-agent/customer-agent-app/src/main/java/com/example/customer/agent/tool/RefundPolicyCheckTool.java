package com.example.customer.agent.tool;

import com.example.customer.agent.order.MockOrderRepository;
import com.example.customer.domain.order.CustomerOrder;
import com.example.customer.domain.order.OrderStatus;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 退款政策检查工具。
 * <p>
 * 只做退款或取消前置政策判断，返回建议动作，不执行真实资金操作或订单状态变更。
 *
 * @author jiangzhibin
 * @since 2026-06-29 15:20:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundPolicyCheckTool {

    public static final String NAME = "refund_policy_check";
    public static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String ERROR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    private static final Duration SELF_SERVICE_REVIEW_WINDOW = Duration.ofDays(30);
    private static final ToolDefinition DEFINITION = ToolDefinition.readOnly(
            NAME,
            "按订单号和租户检查退款或取消前置政策，不执行真实退款",
            List.of(
                    ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号"),
                    ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID")));

    private final MockOrderRepository orderRepository;

    /**
     * 返回退款政策检查工具定义。
     *
     * @return 工具定义
     */
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * 检查订单是否可进入退款或取消审批。
     *
     * @param orderId 订单号
     * @param tenantId 租户标识
     * @return 工具结果
     */
    public ToolResult check(String orderId, String tenantId) {
        var normalizedOrderId = normalize(orderId, "orderId");
        var normalizedTenantId = normalize(tenantId, "tenantId");
        if (normalizedOrderId == null || normalizedTenantId == null) {
            return invalidArgument(normalizedOrderId == null ? "orderId" : "tenantId");
        }

        log.info("tool_refund_policy_check_start orderId={} tenantId={}", normalizedOrderId, normalizedTenantId);
        return orderRepository.findByIdAndTenantId(normalizedOrderId, normalizedTenantId)
                .map(this::policyResult)
                .orElseGet(() -> notFound(normalizedOrderId, normalizedTenantId));
    }

    private ToolResult policyResult(CustomerOrder order) {
        var decision = decide(order);
        log.info(
                "tool_refund_policy_check_success orderId={} tenantId={} decision={}",
                order.id(),
                order.tenantId(),
                decision.policyDecision());
        return ToolResult.succeeded(
                NAME,
                Map.of(
                        "orderId", order.id(),
                        "tenantId", order.tenantId(),
                        "orderStatus", order.status().name(),
                        "policyDecision", decision.policyDecision(),
                        "recommendedAction", decision.recommendedAction(),
                        "reason", decision.reason(),
                        "fundOperationExecuted", false));
    }

    private PolicyDecision decide(CustomerOrder order) {
        if (order.status() == OrderStatus.REFUNDED) {
            return new PolicyDecision(
                    "NOT_ELIGIBLE",
                    "EXPLAIN_POLICY",
                    "订单已退款，不能重复发起退款。");
        }
        if (order.status() == OrderStatus.CANCELLED) {
            return new PolicyDecision(
                    "NOT_ELIGIBLE",
                    "EXPLAIN_POLICY",
                    "订单已取消，不能重复发起取消或退款。");
        }
        if (order.status() != OrderStatus.PAID) {
            return new PolicyDecision(
                    "NOT_ELIGIBLE",
                    "EXPLAIN_POLICY",
                    "订单当前不是已支付状态，暂不能进入退款审批。");
        }
        var paidDuration = Duration.between(order.paidAt(), Instant.now());
        if (paidDuration.compareTo(SELF_SERVICE_REVIEW_WINDOW) > 0) {
            return new PolicyDecision(
                    "REQUIRES_MANUAL_APPROVAL",
                    "ESCALATE_TO_HUMAN_REVIEW",
                    "订单支付时间超过 30 天，需要人工复核退款政策。");
        }
        return new PolicyDecision(
                "ELIGIBLE_FOR_REVIEW",
                "CREATE_APPROVAL_REQUEST",
                "订单处于退款政策窗口内，可进入人工审批流程。");
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("tool_refund_policy_check_invalid_argument field={}", fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private ToolResult notFound(String orderId, String tenantId) {
        log.warn("tool_refund_policy_check_not_found orderId={} tenantId={}", orderId, tenantId);
        return ToolResult.failed(
                NAME,
                ERROR_ORDER_NOT_FOUND,
                "订单不存在或不属于当前租户: orderId=%s, tenantId=%s".formatted(orderId, tenantId));
    }

    private ToolResult invalidArgument(String fieldName) {
        return ToolResult.failed(NAME, ERROR_INVALID_ARGUMENT, "缺少必填参数: " + fieldName);
    }

    private record PolicyDecision(String policyDecision, String recommendedAction, String reason) {
    }
}
