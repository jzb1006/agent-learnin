package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimalMcpServerApplicationTest {

    @Test
    void shouldRegisterPingTool() {
        var server = MinimalMcpServerApplication.createServer();

        var tools = server.listTools();

        assertTrue(tools.stream().anyMatch(tool -> "ping".equals(tool.name())));
        var ping = tools.stream()
            .filter(tool -> "ping".equals(tool.name()))
            .findFirst()
            .orElseThrow();
        assertEquals("object", ping.inputSchema().get("type"));
        assertEquals(Boolean.FALSE, ping.inputSchema().get("additionalProperties"));
        assertEquals(Boolean.TRUE, ping.annotations().readOnlyHint());
        assertEquals(Boolean.TRUE, ping.annotations().idempotentHint());
        assertEquals(Boolean.FALSE, ping.annotations().destructiveHint());
    }

    @Test
    void shouldReturnPongForPingToolCall() {
        var specification = MinimalMcpServerApplication.createPingToolSpecification();

        var result = specification.callHandler().apply(null, McpSchema.CallToolRequest.builder()
            .name("ping")
            .arguments(Map.of())
            .build());

        assertFalse(result.isError());
        assertEquals("pong", ((McpSchema.TextContent) result.content().getFirst()).text());
    }
}
