package io.github.jiangzhibin.agentlearning.tool;

import org.junit.jupiter.api.Assumptions;
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

class LocalCodeSearchToolTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path outsideRoot;

    @Test
    void shouldSearchJavaFilesUnderAllowedRoot() throws IOException {
        var sourceFile = writeFile(
            tempDir.resolve("src/main/java/com/example/App.java"),
            """
                class App {
                    void connect() { System.out.println("HikariPool timeout"); }
                }
                """
        );
        writeFile(tempDir.resolve("README.md"), "HikariPool should not be searched");
        var tool = new LocalCodeSearchTool(tempDir);

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", "HikariPool")));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals(1, result.evidence().size());
        assertTrue(result.evidence().getFirst().source().endsWith(relative(sourceFile) + ":2"));
        assertTrue(result.evidence().getFirst().content().contains("HikariPool timeout"));
    }

    @Test
    void shouldRejectBlankKeyword() {
        var tool = new LocalCodeSearchTool(tempDir);

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", " ")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertEquals("INVALID_ARGUMENTS", result.errorCode());
        assertTrue(result.errorMessage().contains("keyword"));
    }

    @Test
    void shouldRejectOverlongKeyword() {
        var tool = new LocalCodeSearchTool(tempDir);
        var overlongKeyword = "x".repeat(129);

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", overlongKeyword)));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("keyword"));
        assertTrue(result.errorMessage().contains("128"));
    }

    @Test
    void shouldRejectUnexpectedToolName() {
        var tool = new LocalCodeSearchTool(tempDir);

        var result = tool.execute(new ToolCall("read_config", Map.of("keyword", "HikariPool")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("search_code"));
    }

    @Test
    void shouldClipSearchResults() throws IOException {
        for (var index = 0; index < 6; index++) {
            writeFile(
                tempDir.resolve("src/main/java/com/example/Service" + index + ".java"),
                "class Service" + index + " { String marker = \"HikariPool\"; }"
            );
        }
        var tool = new LocalCodeSearchTool(tempDir);

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", "HikariPool")));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals(5, result.evidence().size());
        assertTrue(result.summary().contains("返回前 5 个"));
    }

    @Test
    void shouldNotReadThroughSymbolicLinkOutsideAllowedRoot() throws IOException {
        var outsideFile = writeFile(
            outsideRoot.resolve("Leak.java"),
            "class Leak { String secret = \"OUTSIDE_ONLY_KEYWORD\"; }"
        );
        var link = tempDir.resolve("src/main/java/com/example/Leak.java");
        Files.createDirectories(link.getParent());
        try {
            Files.createSymbolicLink(link, outsideFile);
        } catch (UnsupportedOperationException | IOException exception) {
            Assumptions.assumeTrue(false, "当前文件系统不支持创建符号链接");
        }
        var tool = new LocalCodeSearchTool(tempDir);

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", "OUTSIDE_ONLY_KEYWORD")));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals(1, result.evidence().size());
        assertFalse(result.evidence().getFirst().source().endsWith("Leak.java:1"));
        assertTrue(result.evidence().getFirst().content().contains("未找到匹配片段"));
    }

    private Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String relative(Path path) {
        return tempDir.relativize(path).toString();
    }
}
