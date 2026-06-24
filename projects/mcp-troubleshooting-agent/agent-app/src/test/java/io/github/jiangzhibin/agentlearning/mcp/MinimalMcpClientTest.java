package io.github.jiangzhibin.agentlearning.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimalMcpClientTest {

    @Test
    void shouldDiscoverAndCallPingThroughStdioMcpServer() {
        var serverJar = Path.of("../mcp-server/target/mcp-server-0.1.0-SNAPSHOT.jar");
        var client = new MinimalMcpClient("java", "-jar", serverJar.toString());

        try (client) {
            var tools = client.listTools();
            var pong = client.callTextTool("ping");

            assertTrue(tools.stream().anyMatch(tool -> "ping".equals(tool.name())));
            assertEquals("pong", pong);
        }
    }
}
