package com.example.customer.mcp.tool;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import com.example.customer.domain.order.CustomerOrder;
import com.example.customer.domain.order.OrderStatus;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * 客服 MCP 只读工具。
 * <p>
 * 暴露 Day 22 P0 MCP tools，并用统一 {@link ToolResult} 返回结构化成功或失败结果。
 *
 * @author jiangzhibin
 * @since 2026-06-30 16:56:00
 */
@Component
@Slf4j
public class CustomerMcpTools {

    private static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    private static final String ERROR_KNOWLEDGE_NOT_FOUND = "KNOWLEDGE_NOT_FOUND";
    private static final String ERROR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    private static final String ERROR_CATALOG_NOT_FOUND = "CATALOG_NOT_FOUND";
    private static final int DEFAULT_TOP_K = 3;
    private static final Duration SELF_SERVICE_REVIEW_WINDOW = Duration.ofDays(30);

    private final List<CustomerMcpKnowledgeItem> knowledgeItems;
    private final List<CustomerOrder> orders;

    public CustomerMcpTools() {
        this(demoKnowledgeItems(), demoOrders());
    }

    CustomerMcpTools(List<CustomerMcpKnowledgeItem> knowledgeItems, List<CustomerOrder> orders) {
        this.knowledgeItems = List.copyOf(Objects.requireNonNull(knowledgeItems, "knowledge items must not be null"));
        this.orders = List.copyOf(Objects.requireNonNull(orders, "orders must not be null"));
    }

    /**
     * 创建带 demo 数据的 MCP 工具实例。
     *
     * @return MCP 工具实例
     */
    static CustomerMcpTools withDemoData() {
        return new CustomerMcpTools(demoKnowledgeItems(), demoOrders());
    }

    /**
     * 按租户检索 FAQ、政策和产品知识。
     *
     * @param tenantId 租户标识
     * @param query 用户问题
     * @param topK 最大召回条数
     * @return 工具结果
     */
    @McpTool(
            name = "kb_search",
            description = "按租户检索 FAQ、政策和产品知识",
            annotations = @McpTool.McpAnnotations(
                    title = "知识库检索",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResult kbSearch(
            @McpToolParam(description = "租户 ID") String tenantId,
            @McpToolParam(description = "用户问题") String query,
            @McpToolParam(description = "最大召回条数", required = false) Integer topK) {
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId", "kb_search");
        var normalizedQuery = normalizeRequired(query, "query", "kb_search");
        if (normalizedTenantId == null || normalizedQuery == null) {
            return failed("kb_search", ERROR_INVALID_ARGUMENT, "缺少或非法参数: "
                    + (normalizedTenantId == null ? "tenantId" : "query"));
        }
        var safeTopK = topK == null ? DEFAULT_TOP_K : Math.max(1, topK);
        var normalizedQueryForSearch = normalizedQuery.toLowerCase(Locale.ROOT);
        var matches = knowledgeItems.stream()
                .filter(item -> normalizedTenantId.equals(item.tenantId()))
                .filter(item -> matches(item, normalizedQueryForSearch))
                .limit(safeTopK)
                .map(this::knowledgePayload)
                .toList();
        if (matches.isEmpty()) {
            return failed("kb_search", ERROR_KNOWLEDGE_NOT_FOUND, "未找到匹配知识: tenantId=" + normalizedTenantId);
        }
        log.info("mcp_kb_search_success tenantId={} queryLength={} count={}",
                normalizedTenantId,
                normalizedQuery.length(),
                matches.size());
        return ToolResult.succeeded(
                "kb_search",
                Map.of(
                        "tenantId", normalizedTenantId,
                        "query", normalizedQuery,
                        "matches", matches));
    }

    /**
     * 按租户和订单号查询订单。
     *
     * @param tenantId 租户标识
     * @param orderId 订单号
     * @return 工具结果
     */
    @McpTool(
            name = "order_lookup",
            description = "按租户和订单号查询订单状态",
            annotations = @McpTool.McpAnnotations(
                    title = "订单查询",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResult orderLookup(
            @McpToolParam(description = "租户 ID") String tenantId,
            @McpToolParam(description = "订单号") String orderId) {
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId", "order_lookup");
        var normalizedOrderId = normalizeRequired(orderId, "orderId", "order_lookup");
        if (normalizedTenantId == null || normalizedOrderId == null) {
            return failed("order_lookup", ERROR_INVALID_ARGUMENT, "缺少必填参数: "
                    + (normalizedTenantId == null ? "tenantId" : "orderId"));
        }
        return findOrder(normalizedTenantId, normalizedOrderId)
                .map(order -> ToolResult.succeeded("order_lookup", orderPayload(order)))
                .orElseGet(() -> failed(
                        "order_lookup",
                        ERROR_ORDER_NOT_FOUND,
                        "订单不存在或不属于当前租户: orderId=%s, tenantId=%s"
                                .formatted(normalizedOrderId, normalizedTenantId)));
    }

    /**
     * 按租户查询课程、政策和 FAQ 目录。
     *
     * @param tenantId 租户标识
     * @param category 知识分类
     * @return 工具结果
     */
    @McpTool(
            name = "course_catalog",
            description = "按租户查询可用课程、政策和 FAQ 目录",
            annotations = @McpTool.McpAnnotations(
                    title = "课程目录",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResult courseCatalog(
            @McpToolParam(description = "租户 ID") String tenantId,
            @McpToolParam(description = "知识分类，可选 PRODUCT、POLICY、FAQ", required = false) String category) {
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId", "course_catalog");
        if (normalizedTenantId == null) {
            return failed("course_catalog", ERROR_INVALID_ARGUMENT, "缺少或非法参数: tenantId");
        }
        var categoryFilter = parseCategory(category);
        if (categoryFilter.invalid()) {
            return failed("course_catalog", ERROR_INVALID_ARGUMENT, "缺少或非法参数: category");
        }
        var items = knowledgeItems.stream()
                .filter(item -> normalizedTenantId.equals(item.tenantId()))
                .filter(item -> categoryFilter.value() == null || categoryFilter.value() == item.category())
                .map(this::catalogPayload)
                .toList();
        if (items.isEmpty()) {
            return failed("course_catalog", ERROR_CATALOG_NOT_FOUND,
                    "未找到可用目录: tenantId=%s, category=%s".formatted(normalizedTenantId, categoryFilter.label()));
        }
        return ToolResult.succeeded(
                "course_catalog",
                Map.of(
                        "tenantId", normalizedTenantId,
                        "category", categoryFilter.label(),
                        "items", items));
    }

    /**
     * 检查退款政策，不执行真实资金操作。
     *
     * @param tenantId 租户标识
     * @param orderId 订单号
     * @return 工具结果
     */
    @McpTool(
            name = "refund_policy_check",
            description = "按租户和订单号检查退款政策，不执行真实资金操作",
            annotations = @McpTool.McpAnnotations(
                    title = "退款政策检查",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResult refundPolicyCheck(
            @McpToolParam(description = "租户 ID") String tenantId,
            @McpToolParam(description = "订单号") String orderId) {
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId", "refund_policy_check");
        var normalizedOrderId = normalizeRequired(orderId, "orderId", "refund_policy_check");
        if (normalizedTenantId == null || normalizedOrderId == null) {
            return failed("refund_policy_check", ERROR_INVALID_ARGUMENT, "缺少必填参数: "
                    + (normalizedTenantId == null ? "tenantId" : "orderId"));
        }
        return findOrder(normalizedTenantId, normalizedOrderId)
                .map(this::refundPolicyPayload)
                .orElseGet(() -> failed(
                        "refund_policy_check",
                        ERROR_ORDER_NOT_FOUND,
                        "订单不存在或不属于当前租户: orderId=%s, tenantId=%s"
                                .formatted(normalizedOrderId, normalizedTenantId)));
    }

    private ToolResult refundPolicyPayload(CustomerOrder order) {
        var decision = decide(order);
        return ToolResult.succeeded(
                "refund_policy_check",
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
            return new PolicyDecision("NOT_ELIGIBLE", "EXPLAIN_POLICY", "订单已退款，不能重复发起退款。");
        }
        if (order.status() == OrderStatus.CANCELLED) {
            return new PolicyDecision("NOT_ELIGIBLE", "EXPLAIN_POLICY", "订单已取消，不能重复发起取消或退款。");
        }
        if (order.status() != OrderStatus.PAID) {
            return new PolicyDecision("NOT_ELIGIBLE", "EXPLAIN_POLICY", "订单当前不是已支付状态，暂不能进入退款审批。");
        }
        if (Duration.between(order.paidAt(), Instant.now()).compareTo(SELF_SERVICE_REVIEW_WINDOW) > 0) {
            return new PolicyDecision("REQUIRES_MANUAL_APPROVAL", "ESCALATE_TO_HUMAN_REVIEW", "订单支付时间超过 30 天，需要人工复核退款政策。");
        }
        return new PolicyDecision("ELIGIBLE_FOR_REVIEW", "CREATE_APPROVAL_REQUEST", "订单处于退款政策窗口内，可进入人工审批流程。");
    }

    private java.util.Optional<CustomerOrder> findOrder(String tenantId, String orderId) {
        return orders.stream()
                .filter(order -> order.id().equals(orderId))
                .filter(order -> order.tenantId().equals(tenantId))
                .findFirst();
    }

    private boolean matches(CustomerMcpKnowledgeItem item, String query) {
        return item.title().toLowerCase(Locale.ROOT).contains(query)
                || item.content().toLowerCase(Locale.ROOT).contains(query)
                || item.category().name().toLowerCase(Locale.ROOT).contains(query);
    }

    private Map<String, Object> knowledgePayload(CustomerMcpKnowledgeItem item) {
        return Map.of(
                "itemId", item.itemId(),
                "title", item.title(),
                "source", item.source(),
                "category", item.category().name(),
                "content", item.content(),
                "score", 1.0);
    }

    private Map<String, Object> catalogPayload(CustomerMcpKnowledgeItem item) {
        return Map.of(
                "itemId", item.itemId(),
                "category", item.category().name(),
                "title", item.title(),
                "source", item.source());
    }

    private Map<String, Object> orderPayload(CustomerOrder order) {
        return Map.of(
                "orderId", order.id(),
                "tenantId", order.tenantId(),
                "customerId", order.customerId(),
                "productName", order.productName(),
                "status", order.status().name(),
                "paidAt", order.paidAt().toString());
    }

    private CategoryFilter parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return new CategoryFilter(null, "ALL", false);
        }
        var label = DomainText.requireNonBlank(category, "category").toUpperCase(Locale.ROOT);
        try {
            return new CategoryFilter(KnowledgeCategory.valueOf(label), label, false);
        } catch (IllegalArgumentException exception) {
            return new CategoryFilter(null, label, true);
        }
    }

    private String normalizeRequired(String value, String fieldName, String toolName) {
        if (value == null || value.isBlank()) {
            log.warn("mcp_tool_invalid_argument tool={} field={}", toolName, fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private ToolResult failed(String toolName, String errorCode, String errorMessage) {
        return ToolResult.failed(toolName, errorCode, errorMessage);
    }

    private static List<CustomerMcpKnowledgeItem> demoKnowledgeItems() {
        return List.of(
                new CustomerMcpKnowledgeItem(
                        "kb-policy-refund",
                        "tenant-demo",
                        KnowledgeCategory.POLICY,
                        "退款政策",
                        "knowledge-base/default/policies/refund-policy.md",
                        "支付 30 天内的订单可进入人工审批流程，系统不会直接执行真实退款。"),
                new CustomerMcpKnowledgeItem(
                        "kb-product-agent",
                        "tenant-demo",
                        KnowledgeCategory.PRODUCT,
                        "企业级 AI Agent 实战营",
                        "knowledge-base/default/products/enterprise-ai-agent.md",
                        "课程覆盖 Spring Boot、Spring AI、RAG、MCP、安全审批和可观测。"),
                new CustomerMcpKnowledgeItem(
                        "kb-faq-learning",
                        "tenant-demo",
                        KnowledgeCategory.FAQ,
                        "新手是否适合学习",
                        "knowledge-base/default/faq/beginner.md",
                        "有 Java 后端经验的学习者可以按 30 天计划逐步完成企业客服 Agent。"),
                new CustomerMcpKnowledgeItem(
                        "kb-other-policy",
                        "tenant-other",
                        KnowledgeCategory.POLICY,
                        "其他租户退款政策",
                        "knowledge-base/tenant-other/policies/refund-policy.md",
                        "其他租户知识不能被 tenant-demo 检索到。"));
    }

    private static List<CustomerOrder> demoOrders() {
        var now = Instant.now();
        return List.of(
                CustomerOrder.paid(
                        "order-1001",
                        "tenant-demo",
                        "customer-1001",
                        "企业级 AI Agent 实战营",
                        now.minus(Duration.ofDays(7))),
                CustomerOrder.paid(
                        "order-legacy-paid",
                        "tenant-demo",
                        "customer-1002",
                        "企业级 AI Agent 架构班",
                        now.minus(Duration.ofDays(120))),
                new CustomerOrder(
                        "order-refunded",
                        "tenant-demo",
                        "customer-1003",
                        "企业级 AI Agent 实战营",
                        OrderStatus.REFUNDED,
                        now.minus(Duration.ofDays(7))));
    }

    private record CategoryFilter(KnowledgeCategory value, String label, boolean invalid) {
    }

    private record PolicyDecision(String policyDecision, String recommendedAction, String reason) {
    }
}
