package com.example.customer.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StdioMcpToolClientIntegrationTest {

    private static final Path MCP_SERVER_JAR = Path.of(
            "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar");

    @Test
    void shouldDiscoverAndCallOrderLookupThroughRealMcpServerProcess() {
        assertThat(Files.isRegularFile(MCP_SERVER_JAR))
                .as("先执行 mvn -pl customer-mcp-server -am package 构建真实 MCP Server JAR")
                .isTrue();

        try (var client = new StdioMcpToolClient("java", serverArgs(), Map.of(), Duration.ofSeconds(10))) {
            var tools = client.listTools();
            var response = client.call(new McpToolCallRequest(
                    McpToolNames.ORDER_LOOKUP,
                    Map.of("tenantId", "tenant-demo", "orderId", "order-1001")));

            assertThat(tools)
                    .extracting(tool -> tool.name())
                    .containsExactlyInAnyOrder(
                            McpToolNames.KB_SEARCH,
                            McpToolNames.ORDER_LOOKUP,
                            McpToolNames.COURSE_CATALOG,
                            McpToolNames.REFUND_POLICY_CHECK);
            assertThat(response.toolName()).isEqualTo(McpToolNames.ORDER_LOOKUP);
            assertThat(response.result().succeeded()).isTrue();
            assertThat(response.result().payload())
                    .containsEntry("tenantId", "tenant-demo")
                    .containsEntry("orderId", "order-1001")
                    .containsEntry("status", "PAID");
        }
    }

    private static java.util.List<String> serverArgs() {
        return java.util.List.of("-jar", MCP_SERVER_JAR.toString());
    }
}
