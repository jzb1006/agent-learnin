package io.github.jiangzhibin.agentlearning.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 最小 MCP Client。
 * <p>
 * 该客户端覆盖本地 stdio MCP Server 的工具发现和文本工具调用闭环。
 *
 * @author jiangzhibin
 * @since 2026-06-23 18:33:02
 */
public final class MinimalMcpClient implements AutoCloseable {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final McpSyncClient client;
    private final List<String> serverErrors = new ArrayList<>();

    /**
     * 创建最小 MCP Client。
     *
     * @param command 服务端启动命令
     * @param args 服务端启动参数
     */
    public MinimalMcpClient(String command, String... args) {
        this(command, Map.of(), args);
    }

    /**
     * 创建最小 MCP Client。
     *
     * @param command 服务端启动命令
     * @param environment 服务端进程环境变量
     * @param args 服务端启动参数
     */
    public MinimalMcpClient(String command, Map<String, String> environment, String... args) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("MCP Server 启动命令不能为空");
        }
        if (environment == null) {
            throw new IllegalArgumentException("MCP Server 环境变量不能为空");
        }
        var parameters = ServerParameters.builder(command)
            .env(environment)
            .args(Arrays.asList(args))
            .build();
        var transport = new StdioClientTransport(parameters, McpJsonDefaults.getMapper());
        transport.setStdErrorHandler(serverErrors::add);
        this.client = McpClient.sync(transport)
            .clientInfo(new McpSchema.Implementation("agent-app", "0.1.0"))
            .requestTimeout(REQUEST_TIMEOUT)
            .initializationTimeout(REQUEST_TIMEOUT)
            .build();
        try {
            this.client.initialize();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("MCP Client 初始化失败，服务端 stderr：" + String.join("\n", serverErrors), exception);
        }
    }

    /**
     * 读取 MCP Server 暴露的工具列表。
     *
     * @return 工具列表
     */
    public List<McpSchema.Tool> listTools() {
        return client.listTools().tools();
    }

    /**
     * 调用无参文本工具。
     *
     * @param toolName 工具名称
     * @return 第一段文本内容
     */
    public String callTextTool(String toolName) {
        return callTextTool(toolName, Map.of());
    }

    /**
     * 调用带参数文本工具。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 第一段文本内容
     */
    public String callTextTool(String toolName, Map<String, Object> arguments) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("工具参数不能为空");
        }
        var result = client.callTool(McpSchema.CallToolRequest.builder()
            .name(toolName)
            .arguments(arguments)
            .build());
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("MCP 工具调用失败：" + firstText(result));
        }
        return firstText(result);
    }

    @Override
    public void close() {
        client.closeGracefully();
    }

    private String firstText(McpSchema.CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) {
            return "";
        }
        if (result.content().getFirst() instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        return result.content().getFirst().toString();
    }
}
