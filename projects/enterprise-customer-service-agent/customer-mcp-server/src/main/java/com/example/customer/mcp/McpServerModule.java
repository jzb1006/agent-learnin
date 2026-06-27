package com.example.customer.mcp;

/**
 * 客服订单 MCP Server 模块锚点。
 * <p>
 * Day 02 仅建立模块边界，MCP tools/resources/prompts 会在后续阶段逐步实现。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:29:00
 */
public final class McpServerModule {

    private static final String MODULE_NAME = "customer-mcp-server";

    private McpServerModule() {
    }

    /**
     * 返回 MCP Server 模块名称。
     *
     * @return 模块名称
     */
    public static String name() {
        return MODULE_NAME;
    }
}
