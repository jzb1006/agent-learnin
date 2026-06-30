package com.example.customer.mcp.tool;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import java.util.List;
import java.util.Optional;

/**
 * 客服 MCP 工具目录。
 * <p>
 * 固定 Day 22 默认暴露的 P0 只读工具集合，避免低风险写工具和高风险动作被 MCP 默认发现。
 *
 * @author jiangzhibin
 * @since 2026-06-30 16:55:00
 */
public record CustomerMcpToolCatalog(List<ToolDefinition> definitions) {

    private static final CustomerMcpToolCatalog P0 = new CustomerMcpToolCatalog(List.of(
            ToolDefinition.readOnly(
                    "kb_search",
                    "按租户检索 FAQ、政策和产品知识",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("query", ToolParameterType.STRING, "用户问题"),
                            ToolParameterSchema.optional("topK", ToolParameterType.NUMBER, "最大召回条数"))),
            ToolDefinition.readOnly(
                    "order_lookup",
                    "按租户和订单号查询订单状态",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号"))),
            ToolDefinition.readOnly(
                    "course_catalog",
                    "按租户查询可用课程、政策和 FAQ 目录",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.optional("category", ToolParameterType.STRING, "知识分类，可选 PRODUCT、POLICY、FAQ"))),
            ToolDefinition.readOnly(
                    "refund_policy_check",
                    "按租户和订单号检查退款政策，不执行真实资金操作",
                    List.of(
                            ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                            ToolParameterSchema.required("orderId", ToolParameterType.STRING, "订单号")))));

    /**
     * 返回 Day 22 默认 P0 工具目录。
     *
     * @return P0 工具目录
     */
    public static CustomerMcpToolCatalog p0() {
        return P0;
    }

    /**
     * 返回工具名称列表。
     *
     * @return 工具名称列表
     */
    public List<String> toolNames() {
        return definitions.stream()
                .map(ToolDefinition::name)
                .toList();
    }

    /**
     * 查询工具定义。
     *
     * @param toolName 工具名称
     * @return 工具定义
     */
    public Optional<ToolDefinition> findDefinition(String toolName) {
        var normalizedToolName = DomainText.requireNonBlank(toolName, "tool name");
        return definitions.stream()
                .filter(definition -> definition.name().equals(normalizedToolName))
                .findFirst();
    }

    /**
     * 查询必然存在的工具定义。
     *
     * @param toolName 工具名称
     * @return 工具定义
     */
    public ToolDefinition requireDefinition(String toolName) {
        return findDefinition(toolName)
                .orElseThrow(() -> new IllegalArgumentException("unknown MCP tool: " + toolName));
    }

    public CustomerMcpToolCatalog {
        definitions = List.copyOf(definitions);
    }
}
