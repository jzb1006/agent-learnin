package com.example.customer.agent.mcp;

import com.example.customer.domain.support.DomainText;
import java.util.Map;
import java.util.Objects;

/**
 * MCP 工具调用请求。
 *
 * @param toolName 工具名称
 * @param arguments 工具参数
 * @author jiangzhibin
 * @since 2026-06-30 18:24:00
 */
public record McpToolCallRequest(String toolName, Map<String, Object> arguments) {

    public McpToolCallRequest {
        toolName = DomainText.requireNonBlank(toolName, "MCP tool name");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "MCP tool arguments must not be null"));
    }
}
