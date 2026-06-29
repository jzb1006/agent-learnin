package com.example.customer.agent.tool;

import com.example.customer.agent.order.MockOrderRepository;
import com.example.customer.domain.order.CustomerOrder;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单查询工具。
 * <p>
 * 按订单号和租户标识读取订单摘要，作为 Agent 后续 Tool Calling 的只读业务工具。
 *
 * @author jiangzhibin
 * @since 2026-06-29 14:28:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLookupTool {

    public static final String NAME = "order_lookup";
    public static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String ERROR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    private static final ToolDefinition DEFINITION = ToolDefinition.readOnly(
            NAME,
            "按订单号和租户查询订单状态",
            List.of(
                    ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号"),
                    ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID")));

    private final MockOrderRepository orderRepository;

    /**
     * 返回订单查询工具定义。
     *
     * @return 工具定义
     */
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * 查询订单。
     *
     * @param orderId 订单号
     * @param tenantId 租户标识
     * @return 工具结果
     */
    public ToolResult lookup(String orderId, String tenantId) {
        var normalizedOrderId = normalize(orderId, "orderId");
        var normalizedTenantId = normalize(tenantId, "tenantId");
        if (normalizedOrderId == null || normalizedTenantId == null) {
            return invalidArgument(normalizedOrderId == null ? "orderId" : "tenantId");
        }

        log.info("tool_order_lookup_start orderId={} tenantId={}", normalizedOrderId, normalizedTenantId);
        return orderRepository.findByIdAndTenantId(normalizedOrderId, normalizedTenantId)
                .map(this::succeeded)
                .orElseGet(() -> notFound(normalizedOrderId, normalizedTenantId));
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("tool_order_lookup_invalid_argument field={}", fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private ToolResult succeeded(CustomerOrder order) {
        log.info("tool_order_lookup_success orderId={} tenantId={} status={}",
                order.id(),
                order.tenantId(),
                order.status());
        return ToolResult.succeeded(
                NAME,
                Map.of(
                        "orderId", order.id(),
                        "tenantId", order.tenantId(),
                        "customerId", order.customerId(),
                        "productName", order.productName(),
                        "status", order.status().name(),
                        "paidAt", order.paidAt().toString()));
    }

    private ToolResult notFound(String orderId, String tenantId) {
        log.warn("tool_order_lookup_not_found orderId={} tenantId={}", orderId, tenantId);
        return ToolResult.failed(
                NAME,
                ERROR_ORDER_NOT_FOUND,
                "订单不存在或不属于当前租户: orderId=%s, tenantId=%s".formatted(orderId, tenantId));
    }

    private ToolResult invalidArgument(String fieldName) {
        return ToolResult.failed(
                NAME,
                ERROR_INVALID_ARGUMENT,
                "缺少必填参数: " + fieldName);
    }
}
