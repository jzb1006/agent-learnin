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

class LocalConfigReadToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadConfigFileAndRedactSensitiveValues() throws IOException {
        var projectRoot = tempDir.resolve("project");
        var configFile = writeFile(
            projectRoot.resolve("config/application.yml"),
            """
                server:
                  port: 8080
                deepseek:
                  api-key: sk-live-secret
                spring:
                  datasource:
                    password: root-password
                """
        );
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall(
            "read_config",
            Map.of("path", "config/application.yml")
        ));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertTrue(result.evidence().stream().anyMatch(item -> item.source().endsWith("config/application.yml:2")));
        assertTrue(joinContent(result).contains("port: 8080"));
        assertFalse(joinContent(result).contains("sk-live-secret"));
        assertFalse(joinContent(result).contains("root-password"));
        assertTrue(joinContent(result).contains("api-key: [REDACTED]"));
        assertTrue(joinContent(result).contains("password: [REDACTED]"));
        assertTrue(result.evidence().getFirst().source().startsWith(relative(projectRoot, configFile)));
    }

    @Test
    void shouldRejectPathTraversalOutsideAllowedRoot() throws IOException {
        var projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot);
        writeFile(tempDir.resolve("outside.env"), "TOKEN=outside-secret");
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall("read_config", Map.of("path", "../outside.env")));

        assertEquals(ToolResultStatus.PERMISSION_DENIED, result.status());
        assertFalse(result.errorMessage().contains("outside-secret"));
    }

    @Test
    void shouldRejectAbsolutePathEvenInsideAllowedRoot() throws IOException {
        var projectRoot = tempDir.resolve("project");
        var configFile = writeFile(projectRoot.resolve("application.yml"), "server.port=8080");
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall("read_config", Map.of("path", configFile.toString())));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("相对路径"));
    }

    @Test
    void shouldRejectUnsupportedConfigExtension() throws IOException {
        var projectRoot = tempDir.resolve("project");
        writeFile(projectRoot.resolve("README.md"), "password=secret");
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall("read_config", Map.of("path", "README.md")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("配置文件"));
    }

    @Test
    void shouldNotReadThroughSymbolicLinkOutsideAllowedRoot() throws IOException {
        var projectRoot = tempDir.resolve("project");
        var outsideFile = writeFile(tempDir.resolve("outside.env"), "TOKEN=outside-secret");
        var link = projectRoot.resolve("config/leak.env");
        Files.createDirectories(link.getParent());
        try {
            Files.createSymbolicLink(link, outsideFile);
        } catch (UnsupportedOperationException | IOException exception) {
            Assumptions.assumeTrue(false, "当前文件系统不支持创建符号链接");
        }
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall("read_config", Map.of("path", "config/leak.env")));

        assertEquals(ToolResultStatus.PERMISSION_DENIED, result.status());
        assertFalse(result.errorMessage().contains("outside-secret"));
    }

    @Test
    void shouldRejectUnexpectedToolName() throws IOException {
        var projectRoot = tempDir.resolve("project");
        writeFile(projectRoot.resolve("application.yml"), "server.port=8080");
        var tool = new LocalConfigReadTool(projectRoot);

        var result = tool.execute(new ToolCall("git_history", Map.of("path", "application.yml")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("read_config"));
    }

    private Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String joinContent(ToolResult result) {
        return String.join("\n", result.evidence().stream().map(ToolEvidence::content).toList());
    }

    private String relative(Path root, Path path) {
        return root.relativize(path).toString();
    }
}
