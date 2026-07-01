package com.example.customer.agent.mcp;

/**
 * 客服 MCP 工具名称。
 * <p>
 * 集中保存 Agent App 侧依赖的 MCP 工具契约名称，避免对话编排层直接引用工具实现类。
 *
 * @author jiangzhibin
 * @since 2026-06-30 18:24:00
 */
public final class McpToolNames {

    public static final String KB_SEARCH = "kb_search";
    public static final String ORDER_LOOKUP = "order_lookup";
    public static final String COURSE_CATALOG = "course_catalog";
    public static final String REFUND_POLICY_CHECK = "refund_policy_check";

    private McpToolNames() {
    }
}
