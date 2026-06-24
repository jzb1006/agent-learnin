package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 最小 MCP Server 启动入口。
 * <p>
 * 该入口只注册 Day 12 所需的 ping 工具，用于验证 stdio transport、工具发现和工具调用链路。
 *
 * @author jiangzhibin
 * @since 2026-06-23 18:33:02
 */
public final class MinimalMcpServerApplication {

    static final String TOOL_NAME = "ping";
    private static final String SEARCH_CODE_ROOT_ENV = "MCP_SEARCH_CODE_ROOT";
    private static final String SERVER_NAME = "mcp-troubleshooting-agent";
    private static final String SERVER_VERSION = "0.1.0";

    private MinimalMcpServerApplication() {
    }

    /**
     * 以 stdio transport 启动最小 MCP Server。
     *
     * @param args 命令行参数，当前未使用
     */
    public static void main(String[] args) {
        createServer();
    }

    /**
     * 创建已注册 ping 工具的 MCP Server。
     *
     * @return 同步 MCP Server
     */
    static McpSyncServer createServer() {
        return createServer(resolveSearchCodeRoot());
    }

    private static McpSyncServer createServer(Path searchCodeRoot) {
        var transportProvider = new StdioServerTransportProvider(McpJsonDefaults.getMapper());
        return McpServer.sync(transportProvider)
            .serverInfo(new McpSchema.Implementation(SERVER_NAME, SERVER_VERSION))
            .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
            .tools(createPingToolSpecification(), createSearchCodeToolSpecification(searchCodeRoot))
            .build();
    }

    /**
     * 创建 ping 工具规格，供单元测试验证处理器行为。
     *
     * @return ping 工具规格
     */
    static McpServerFeatures.SyncToolSpecification createPingToolSpecification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(createPingTool())
            .callHandler(createPingHandler())
            .build();
    }

    /**
     * 创建 search_code 工具规格，供单元测试验证处理器行为。
     *
     * @param allowedRoot 允许搜索的根目录
     * @return search_code 工具规格
     */
    static McpServerFeatures.SyncToolSpecification createSearchCodeToolSpecification(Path allowedRoot) {
        return new SearchCodeMcpTool(allowedRoot).specification();
    }

    private static McpSchema.Tool createPingTool() {
        return McpSchema.Tool.builder(TOOL_NAME)
            .description("验证 MCP Server 可达性。无参数，返回 pong。")
            .inputSchema(McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of())
                .required(List.of())
                .additionalProperties(false)
                .build())
            .annotations(McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .openWorldHint(false)
                .build())
            .build();
    }

    private static java.util.function.BiFunction<
        io.modelcontextprotocol.server.McpSyncServerExchange,
        McpSchema.CallToolRequest,
        McpSchema.CallToolResult
    > createPingHandler() {
        return (exchange, request) -> {
            if (!TOOL_NAME.equals(request.name())) {
                return McpSchema.CallToolResult.builder()
                    .addTextContent("工具名称必须是 ping")
                    .isError(true)
                    .build();
            }
            if (request.arguments() != null && !request.arguments().isEmpty()) {
                return McpSchema.CallToolResult.builder()
                    .addTextContent("ping 不接收参数")
                    .isError(true)
                    .build();
            }
            return McpSchema.CallToolResult.builder()
                .addTextContent("pong")
                .isError(false)
                .build();
        };
    }

    private static Path resolveSearchCodeRoot() {
        var configuredRoot = System.getenv(SEARCH_CODE_ROOT_ENV);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            return Path.of(".");
        }
        return Path.of(configuredRoot);
    }
}
