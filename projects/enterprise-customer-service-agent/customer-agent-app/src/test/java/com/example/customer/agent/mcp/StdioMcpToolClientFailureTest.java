package com.example.customer.agent.mcp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpSyncClient;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * stdio MCP 客户端失败路径测试。
 * <p>
 * 验证初始化失败后不会复用半初始化的 MCP SDK client，避免后续请求触发重复订阅或长期超时。
 *
 * @author jiangzhibin
 * @since 2026-07-01 11:24:00
 */
class StdioMcpToolClientFailureTest {

    @Test
    void shouldDiscardHalfInitializedClientAfterInitializationFailure() {
        try (var client = new StdioMcpToolClient(
                "definitely-not-a-mcp-command",
                List.of(),
                Map.of(),
                Duration.ofSeconds(1))) {
            assertThatThrownBy(client::listTools).isInstanceOf(RuntimeException.class);
            assertThat(rawSdkClient(client)).isNull();

            assertThatThrownBy(client::listTools)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageNotContaining("Sinks.many().unicast() sinks only allow a single Subscriber");
            assertThat(rawSdkClient(client)).isNull();
        }
    }

    @Test
    void shouldReturnFailedToolResultWhenCallCannotInitializeMcpServer() {
        try (var client = new StdioMcpToolClient(
                "definitely-not-a-mcp-command",
                List.of(),
                Map.of(),
                Duration.ofSeconds(1))) {
            var response = client.call(new McpToolCallRequest(
                    McpToolNames.ORDER_LOOKUP,
                    Map.of("tenantId", "tenant-demo", "orderId", "order-1001")));

            assertThat(response.toolName()).isEqualTo(McpToolNames.ORDER_LOOKUP);
            assertThat(response.result().succeeded()).isFalse();
            assertThat(response.result().errorCode()).contains("MCP_CLIENT_CALL_FAILED");
            assertThat(rawSdkClient(client)).isNull();
        }
    }

    private McpSyncClient rawSdkClient(StdioMcpToolClient client) {
        try {
            Field field = StdioMcpToolClient.class.getDeclaredField("client");
            field.setAccessible(true);
            return (McpSyncClient) field.get(client);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法读取 StdioMcpToolClient.client 字段", exception);
        }
    }
}
