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
    void shouldRegisterGitHistoryAndReadConfigTools() {
        var server = MinimalMcpServerApplication.createServer();

        var tools = server.listTools();

        assertTrue(tools.stream().anyMatch(tool -> "git_history".equals(tool.name())));
        assertTrue(tools.stream().anyMatch(tool -> "read_config".equals(tool.name())));
        var gitHistory = tools.stream()
            .filter(tool -> "git_history".equals(tool.name()))
            .findFirst()
            .orElseThrow();
        var readConfig = tools.stream()
            .filter(tool -> "read_config".equals(tool.name()))
            .findFirst()
            .orElseThrow();
        assertEquals("object", gitHistory.inputSchema().get("type"));
        assertEquals(Boolean.FALSE, gitHistory.inputSchema().get("additionalProperties"));
        assertTrue(((Map<?, ?>) gitHistory.inputSchema().get("properties")).containsKey("keyword"));
        assertTrue(((Map<?, ?>) gitHistory.inputSchema().get("properties")).containsKey("maxResults"));
        assertEquals(Boolean.TRUE, gitHistory.annotations().readOnlyHint());
        assertEquals(Boolean.TRUE, gitHistory.annotations().idempotentHint());
        assertEquals(Boolean.FALSE, gitHistory.annotations().destructiveHint());
        assertEquals("object", readConfig.inputSchema().get("type"));
        assertEquals(Boolean.FALSE, readConfig.inputSchema().get("additionalProperties"));
        assertTrue(((Map<?, ?>) readConfig.inputSchema().get("properties")).containsKey("path"));
        assertEquals(Boolean.TRUE, readConfig.annotations().readOnlyHint());
        assertEquals(Boolean.TRUE, readConfig.annotations().idempotentHint());
        assertEquals(Boolean.FALSE, readConfig.annotations().destructiveHint());
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

    @Test
    void shouldReturnMcpErrorWhenGitHistoryMaxResultsIsInvalid() throws IOException {
        var specification = MinimalMcpServerApplication.createGitHistoryToolSpecification(tempDir);

        var result = specification.callHandler().apply(null, McpSchema.CallToolRequest.builder()
            .name("git_history")
            .arguments(Map.of("maxResults", 0))
            .build());

        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("\"status\":\"INVALID_ARGUMENTS\""));
        assertTrue(text.contains("maxResults"));
    }

    @Test
    void shouldReturnMcpErrorWhenReadConfigPathEscapesAllowedRoot() throws IOException {
        writeFile(tempDir.resolve("outside.env"), "TOKEN=outside-secret");
        var projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        var specification = MinimalMcpServerApplication.createReadConfigToolSpecification(projectRoot);

        var result = specification.callHandler().apply(null, McpSchema.CallToolRequest.builder()
            .name("read_config")
            .arguments(Map.of("path", "../outside.env"))
            .build());

        assertTrue(result.isError());
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        assertTrue(text.contains("\"status\":\"PERMISSION_DENIED\""));
        assertTrue(text.contains("允许根目录"));
        assertFalse(text.contains("outside-secret"));
    }

    private Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String relative(Path path) {
        return tempDir.relativize(path).toString();
    }
}
