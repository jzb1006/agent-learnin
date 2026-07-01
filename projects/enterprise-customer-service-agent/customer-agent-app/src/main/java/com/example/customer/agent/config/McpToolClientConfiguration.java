package com.example.customer.agent.config;

import com.example.customer.agent.mcp.McpToolClient;
import com.example.customer.agent.mcp.StdioMcpToolClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具客户端配置。
 * <p>
 * 统一创建真实 stdio MCP client，避免对话编排层关心 transport 细节。
 *
 * @author jiangzhibin
 * @since 2026-07-01 09:56:00
 */
@Configuration
public class McpToolClientConfiguration {

    /**
     * 创建 MCP 工具客户端。
     *
     * @param properties 客服 Agent 配置
     * @return MCP 工具客户端
     */
    @Bean(destroyMethod = "close")
    public McpToolClient mcpToolClient(CustomerAgentProperties properties) {
        var mcpClient = properties.getMcpClient();
        return switch (mcpClient.getMode()) {
            case STDIO -> new StdioMcpToolClient(
                    mcpClient.getCommand(),
                    List.of("-jar", mcpClient.getServerJar()),
                    Map.of(),
                    Duration.ofSeconds(mcpClient.getRequestTimeoutSeconds()));
        };
    }
}
