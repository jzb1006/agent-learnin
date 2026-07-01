package com.example.customer.agent.mcp;

import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class FakeMcpToolClient implements McpToolClient {

    private static final List<ToolDefinition> TOOL_DEFINITIONS = List.of(
            ToolDefinition.readOnly(
                    McpToolNames.KB_SEARCH,
                    "按租户检索 FAQ、政策和产品知识",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("query", ToolParameterType.STRING, "用户问题"),
                            ToolParameterSchema.optional("topK", ToolParameterType.NUMBER, "最大召回条数"))),
            ToolDefinition.readOnly(
                    McpToolNames.ORDER_LOOKUP,
                    "按租户和订单号查询订单状态",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号"))),
            ToolDefinition.readOnly(
                    McpToolNames.COURSE_CATALOG,
                    "按租户查询可用课程、政策和 FAQ 目录",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.optional("category", ToolParameterType.STRING, "知识分类，可选 PRODUCT、POLICY、FAQ"))),
            ToolDefinition.readOnly(
                    McpToolNames.REFUND_POLICY_CHECK,
                    "按租户和订单号检查退款政策，不执行真实资金操作",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号"))));

    @Override
    public List<ToolDefinition> listTools() {
        return TOOL_DEFINITIONS;
    }

    @Override
    public McpToolCallResponse call(McpToolCallRequest request) {
        var startedAtNanos = System.nanoTime();
        var result = switch (request.toolName()) {
            case McpToolNames.KB_SEARCH -> kbSearch(request.arguments());
            case McpToolNames.ORDER_LOOKUP -> orderLookup(request.arguments());
            case McpToolNames.COURSE_CATALOG -> courseCatalog(request.arguments());
            case McpToolNames.REFUND_POLICY_CHECK -> refundPolicyCheck(request.arguments());
            default -> ToolResult.failed(
                    request.toolName(),
                    "MCP_TOOL_NOT_FOUND",
                    "未知 MCP 工具: " + request.toolName());
        };
        return new McpToolCallResponse(
                request.toolName(),
                result,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos));
    }

    private ToolResult kbSearch(Map<String, Object> arguments) {
        var tenantId = text(arguments, "tenantId");
        var query = text(arguments, "query");
        if ("tenant-kb-api".equals(tenantId) || "tenant-kb-list".equals(tenantId)) {
            return ToolResult.failed(McpToolNames.KB_SEARCH, "KNOWLEDGE_NOT_FOUND", "测试 fake MCP 不读取 App 内知识库索引");
        }
        return ToolResult.succeeded(
                McpToolNames.KB_SEARCH,
                Map.of(
                        "tenantId", tenantId,
                        "query", query,
                        "matches", List.of(Map.of(
                                "title", "课程适合哪些学员",
                                "content", "课程适合有 Java 后端经验的开发者，不建议完全零基础学员直接学习。",
                                "source", "week10/work_v3/datas/data.txt#Q1-Q6"))));
    }

    private ToolResult orderLookup(Map<String, Object> arguments) {
        var tenantId = text(arguments, "tenantId");
        var orderId = text(arguments, "orderId");
        if (!"tenant-demo".equals(tenantId) || !"order-1001".equals(orderId)) {
            return ToolResult.failed(
                    McpToolNames.ORDER_LOOKUP,
                    "ORDER_NOT_FOUND",
                    "订单不存在或不属于当前租户: orderId=%s, tenantId=%s".formatted(orderId, tenantId));
        }
        return ToolResult.succeeded(McpToolNames.ORDER_LOOKUP, orderPayload(tenantId, orderId));
    }

    private ToolResult courseCatalog(Map<String, Object> arguments) {
        return ToolResult.succeeded(
                McpToolNames.COURSE_CATALOG,
                Map.of(
                        "tenantId", text(arguments, "tenantId"),
                        "category", text(arguments, "category"),
                        "items", List.of(Map.of(
                                "title", "企业级 AI Agent 实战营",
                                "category", "PRODUCT",
                                "source", "fake-mcp#course"))));
    }

    private ToolResult refundPolicyCheck(Map<String, Object> arguments) {
        var tenantId = text(arguments, "tenantId");
        var orderId = text(arguments, "orderId");
        if (!"tenant-demo".equals(tenantId) || !"order-1001".equals(orderId)) {
            return ToolResult.failed(
                    McpToolNames.REFUND_POLICY_CHECK,
                    "ORDER_NOT_FOUND",
                    "订单不存在或不属于当前租户: orderId=%s, tenantId=%s".formatted(orderId, tenantId));
        }
        return ToolResult.succeeded(
                McpToolNames.REFUND_POLICY_CHECK,
                Map.of(
                        "orderId", orderId,
                        "tenantId", tenantId,
                        "orderStatus", "PAID",
                        "policyDecision", "ELIGIBLE_FOR_REVIEW",
                        "recommendedAction", "CREATE_APPROVAL_REQUEST",
                        "reason", "订单处于退款政策窗口内，可进入人工审批流程。",
                        "fundOperationExecuted", false));
    }

    private Map<String, Object> orderPayload(String tenantId, String orderId) {
        return Map.of(
                "orderId", orderId,
                "tenantId", tenantId,
                "customerId", "customer-1001",
                "productName", "企业级 AI Agent 实战营",
                "status", "PAID",
                "paidAt", Instant.parse("2026-06-15T10:00:00Z").toString());
    }

    private String text(Map<String, Object> arguments, String name) {
        var value = arguments.get(name);
        return value == null ? "" : value.toString();
    }
}
