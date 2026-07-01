package com.example.customer.agent.mcp;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * stdio MCP 工具客户端。
 * <p>
 * 通过 MCP Java SDK 启动并调用真实的 `customer-mcp-server` 进程，把协议返回转换为领域层工具契约。
 *
 * @author jiangzhibin
 * @since 2026-07-01 09:56:00
 */
@Slf4j
public class StdioMcpToolClient implements McpToolClient, AutoCloseable {

    private static final String ERROR_EMPTY_CONTENT = "MCP_EMPTY_CONTENT";
    private static final String ERROR_NON_TEXT_CONTENT = "MCP_NON_TEXT_CONTENT";
    private static final String ERROR_RESULT_PARSE_FAILED = "MCP_RESULT_PARSE_FAILED";
    private static final String ERROR_TOOL_FAILED_WITHOUT_PAYLOAD = "MCP_TOOL_FAILED_WITHOUT_PAYLOAD";
    private static final String ERROR_CLIENT_CALL_FAILED = "MCP_CLIENT_CALL_FAILED";

    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private McpSyncClient client;
    private List<ToolDefinition> toolDefinitions;

    /**
     * 创建 stdio MCP 工具客户端。
     *
     * @param command 启动命令
     * @param args 启动参数
     * @param env 环境变量
     * @param requestTimeout MCP 请求超时时间
     */
    public StdioMcpToolClient(
            String command,
            List<String> args,
            Map<String, String> env,
            Duration requestTimeout) {
        this.command = DomainText.requireNonBlank(command, "MCP stdio command");
        this.args = List.copyOf(Objects.requireNonNull(args, "MCP stdio args must not be null"));
        this.env = Map.copyOf(Objects.requireNonNull(env, "MCP stdio env must not be null"));
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "MCP request timeout must not be null");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("MCP request timeout must be positive");
        }
    }

    @Override
    public synchronized List<ToolDefinition> listTools() {
        if (toolDefinitions == null) {
            try {
                toolDefinitions = initializeClient().listTools().tools().stream()
                        .map(this::toToolDefinition)
                        .toList();
            } catch (RuntimeException exception) {
                resetClientAfterFailure(exception);
                throw exception;
            }
        }
        return toolDefinitions;
    }

    @Override
    public synchronized McpToolCallResponse call(McpToolCallRequest request) {
        var startedAtNanos = System.nanoTime();
        ToolResult toolResult;
        try {
            var result = initializeClient().callTool(McpSchema.CallToolRequest.builder(request.toolName())
                    .arguments(request.arguments())
                    .build());
            toolResult = toToolResult(request.toolName(), result);
        } catch (RuntimeException exception) {
            resetClientAfterFailure(exception);
            toolResult = ToolResult.failed(
                    request.toolName(),
                    ERROR_CLIENT_CALL_FAILED,
                    "MCP 工具调用失败: " + rootCauseMessage(exception));
        }
        return new McpToolCallResponse(request.toolName(), toolResult, elapsedMillis(startedAtNanos));
    }

    @Override
    public synchronized void close() {
        if (client == null) {
            return;
        }
        try {
            client.closeGracefully();
        } catch (RuntimeException exception) {
            log.warn("mcp_stdio_client_close_failed command={} args={}", command, args, exception);
        } finally {
            client = null;
            toolDefinitions = null;
        }
    }

    private McpSyncClient initializeClient() {
        if (client == null) {
            var serverParameters = ServerParameters.builder(command)
                    .args(args)
                    .env(env)
                    .build();
            var transport = new StdioClientTransport(serverParameters, McpJsonDefaults.getMapper());
            transport.setStdErrorHandler(line -> log.debug("mcp_stdio_server_stderr {}", line));
            var candidate = McpClient.sync(transport)
                    .requestTimeout(requestTimeout)
                    .initializationTimeout(requestTimeout)
                    .build();
            try {
                candidate.initialize();
            } catch (RuntimeException exception) {
                closeClientQuietly(candidate, exception);
                throw new IllegalStateException("MCP stdio client 初始化失败", exception);
            }
            client = candidate;
            log.info("mcp_stdio_client_initialized command={} args={}", command, args);
        }
        return client;
    }

    private void resetClientAfterFailure(RuntimeException exception) {
        closeClientQuietly(client, exception);
        client = null;
        toolDefinitions = null;
    }

    private void closeClientQuietly(McpSyncClient clientToClose, RuntimeException cause) {
        if (clientToClose == null) {
            return;
        }
        try {
            clientToClose.closeGracefully();
        } catch (RuntimeException closeException) {
            cause.addSuppressed(closeException);
            log.warn("mcp_stdio_client_close_failed_after_error command={} args={}", command, args, closeException);
        }
    }

    private String rootCauseMessage(RuntimeException exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private ToolDefinition toToolDefinition(McpSchema.Tool tool) {
        var riskLevel = Boolean.TRUE.equals(tool.annotations() == null ? null : tool.annotations().readOnlyHint())
                ? com.example.customer.domain.tool.ToolRiskLevel.READ_ONLY
                : com.example.customer.domain.tool.ToolRiskLevel.HIGH_RISK;
        return new ToolDefinition(
                tool.name(),
                tool.description() == null || tool.description().isBlank() ? tool.name() : tool.description(),
                parameters(tool.inputSchema()),
                riskLevel,
                com.example.customer.domain.tool.ToolPermission.defaultFor(riskLevel));
    }

    private List<ToolParameterSchema> parameters(Map<String, Object> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            return List.of();
        }
        var required = requiredParameters(inputSchema.get("required"));
        return propertySchemas(inputSchema.get("properties")).entrySet().stream()
                .map(entry -> toParameterSchema(entry.getKey(), entry.getValue(), required.contains(entry.getKey())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> propertySchemas(Object value) {
        if (!(value instanceof Map<?, ?> rawProperties)) {
            return Map.of();
        }
        return rawProperties.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .filter(entry -> entry.getValue() instanceof Map<?, ?>)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> (String) entry.getKey(),
                        entry -> (Map<String, Object>) entry.getValue()));
    }

    private List<String> requiredParameters(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private ToolParameterSchema toParameterSchema(
            String name,
            Map<String, Object> schema,
            boolean required) {
        var description = stringValue(schema.get("description"), name);
        var type = typeOf(stringValue(schema.get("type"), "string"));
        return new ToolParameterSchema(name, type, description, required, false);
    }

    private ToolParameterType typeOf(String type) {
        return switch (type) {
            case "number", "integer" -> ToolParameterType.NUMBER;
            case "boolean" -> ToolParameterType.BOOLEAN;
            case "object" -> ToolParameterType.OBJECT;
            case "array" -> ToolParameterType.ARRAY;
            default -> ToolParameterType.STRING;
        };
    }

    private ToolResult toToolResult(String toolName, McpSchema.CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) {
            return ToolResult.failed(toolName, ERROR_EMPTY_CONTENT, "MCP 工具未返回 content");
        }
        var content = result.content().getFirst();
        if (!(content instanceof McpSchema.TextContent textContent)) {
            return ToolResult.failed(toolName, ERROR_NON_TEXT_CONTENT, "MCP 工具返回了非文本 content");
        }
        try {
            return objectMapper.readValue(textContent.text(), ToolResult.class);
        } catch (JacksonException exception) {
            if (Boolean.TRUE.equals(result.isError())) {
                return ToolResult.failed(toolName, ERROR_TOOL_FAILED_WITHOUT_PAYLOAD, textContent.text());
            }
            throw new IllegalStateException("MCP 工具结果不是合法 ToolResult JSON: " + toolName, exception);
        } catch (IllegalArgumentException exception) {
            return ToolResult.failed(toolName, ERROR_RESULT_PARSE_FAILED, exception.getMessage());
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
