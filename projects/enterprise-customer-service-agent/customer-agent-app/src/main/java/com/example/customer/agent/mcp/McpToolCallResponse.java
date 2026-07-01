package com.example.customer.agent.mcp;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolResult;
import java.util.Objects;

/**
 * MCP 工具调用响应。
 *
 * @param toolName 工具名称
 * @param result 工具结果
 * @param durationMs 调用耗时，单位毫秒
 * @author jiangzhibin
 * @since 2026-06-30 18:24:00
 */
public record McpToolCallResponse(String toolName, ToolResult result, long durationMs) {

    public McpToolCallResponse {
        toolName = DomainText.requireNonBlank(toolName, "MCP tool name");
        Objects.requireNonNull(result, "MCP tool result must not be null");
        if (durationMs < 0) {
            throw new IllegalArgumentException("MCP tool duration must be greater than or equal to 0");
        }
    }
}
