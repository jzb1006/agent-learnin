package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimalMcpServerApplicationTest {

    @TempDir
    Path tempDir;

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

    @Test
    void shouldRegisterSearchCodeTool() {
        var server = MinimalMcpServerApplication.createServer();

        var tools = server.listTools();

        assertTrue(tools.stream().anyMatch(tool -> "search_code".equals(tool.name())));
        var searchCode = tools.stream()
            .filter(tool -> "search_code".equals(tool.name()))
            .findFirst()
            .orElseThrow();
        assertEquals("object", searchCode.inputSchema().get("type"));
        assertEquals(Boolean.FALSE, searchCode.inputSchema().get("additionalProperties"));
        assertTrue(((Map<?, ?>) searchCode.inputSchema().get("properties")).containsKey("keyword"));
        assertEquals(Boolean.TRUE, searchCode.annotations().readOnlyHint());
        assertEquals(Boolean.TRUE, searchCode.annotations().idempotentHint());
        assertEquals(Boolean.FALSE, searchCode.annotations().destructiveHint());
    }

    @Test
    void shouldReturnSearchCodeResultAsJsonText() throws IOException {
        var sourceFile = writeFile(
            tempDir.resolve("src/main/java/com/example/App.java"),
            """
                class App {
                    void connect() { System.out.println("HikariPool timeout"); }
                }
                """
        );
        var specification = MinimalMcpServerApplication.createSearchCodeToolSpecification(tempDir);

        var result = specification.callHandler().apply(null, McpSchema.CallToolRequest.builder()
            .name("search_code")
            .arguments(Map.of("keyword", "HikariPool"))
            .build());

        assertFalse(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("\"status\":\"SUCCESS\""));
        assertTrue(text.contains("\"summary\""));
        assertTrue(text.contains(relative(sourceFile) + ":2"));
        assertTrue(text.contains("HikariPool timeout"));
    }

    @Test
    void shouldReturnMcpErrorWhenSearchCodeKeywordIsBlank() {
        var specification = MinimalMcpServerApplication.createSearchCodeToolSpecification(tempDir);

        var result = specification.callHandler().apply(null, McpSchema.CallToolRequest.builder()
            .name("search_code")
            .arguments(Map.of("keyword", " "))
            .build());

        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("\"status\":\"INVALID_ARGUMENTS\""));
        assertTrue(text.contains("keyword"));
    }

    private Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String relative(Path path) {
        return tempDir.relativize(path).toString();
    }
}
