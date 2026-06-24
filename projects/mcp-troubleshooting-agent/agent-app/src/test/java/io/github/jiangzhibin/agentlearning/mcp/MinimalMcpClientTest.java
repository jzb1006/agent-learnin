package io.github.jiangzhibin.agentlearning.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimalMcpClientTest {

    @TempDir
    Path tempDir;

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

    @Test
    void shouldCallSearchCodeThroughStdioMcpServer() throws IOException {
        var serverJar = Path.of("../mcp-server/target/mcp-server-0.1.0-SNAPSHOT.jar");
        var sourceFile = writeFile(
            tempDir.resolve("src/main/java/com/example/App.java"),
            """
                class App {
                    void connect() { System.out.println("HikariPool timeout"); }
                }
                """
        );
        var client = new MinimalMcpClient(
            "java",
            Map.of("MCP_SEARCH_CODE_ROOT", tempDir.toString()),
            "-jar",
            serverJar.toString()
        );

        try (client) {
            var tools = client.listTools();
            var text = client.callTextTool("search_code", Map.of("keyword", "HikariPool"));

            assertTrue(tools.stream().anyMatch(tool -> "search_code".equals(tool.name())));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains(tempDir.relativize(sourceFile).toString() + ":2"));
            assertTrue(text.contains("HikariPool timeout"));
        }
    }

    private Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
