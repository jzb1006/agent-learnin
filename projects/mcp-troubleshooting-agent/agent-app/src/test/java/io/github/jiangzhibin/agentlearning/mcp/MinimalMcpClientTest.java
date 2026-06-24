package io.github.jiangzhibin.agentlearning.mcp;

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

    @Test
    void shouldCallGitHistoryAndReadConfigThroughStdioMcpServer() throws IOException, InterruptedException {
        var serverJar = Path.of("../mcp-server/target/mcp-server-0.1.0-SNAPSHOT.jar");
        var projectRoot = tempDir.resolve("project");
        initGitRepository(projectRoot);
        writeFile(
            projectRoot.resolve("config/application.yml"),
            """
                server:
                  port: 8080
                deepseek:
                  api-key: sk-live-secret
                """
        );
        commit(projectRoot, "config/application.yml", "feat: add gateway config");
        var client = new MinimalMcpClient(
            "java",
            Map.of(
                "MCP_SEARCH_CODE_ROOT", projectRoot.toString(),
                "MCP_GIT_HISTORY_ROOT", projectRoot.toString(),
                "MCP_READ_CONFIG_ROOT", projectRoot.toString()
            ),
            "-jar",
            serverJar.toString()
        );

        try (client) {
            var tools = client.listTools();
            var gitHistory = client.callTextTool("git_history", Map.of("keyword", "gateway", "maxResults", 3));
            var readConfig = client.callTextTool("read_config", Map.of("path", "config/application.yml"));

            assertTrue(tools.stream().anyMatch(tool -> "git_history".equals(tool.name())));
            assertTrue(tools.stream().anyMatch(tool -> "read_config".equals(tool.name())));
            assertTrue(gitHistory.contains("\"status\":\"SUCCESS\""));
            assertTrue(gitHistory.contains("feat: add gateway config"));
            assertTrue(readConfig.contains("\"status\":\"SUCCESS\""));
            assertTrue(readConfig.contains("config/application.yml:2"));
            assertTrue(readConfig.contains("port: 8080"));
            assertFalse(readConfig.contains("sk-live-secret"));
            assertTrue(readConfig.contains("api-key: [REDACTED]"));
        }
    }

    private Path writeFile(Path path, String content) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void initGitRepository(Path repo) throws IOException, InterruptedException {
        Files.createDirectories(repo);
        runGit(repo, "init");
        runGit(repo, "config", "user.name", "Agent Learner");
        runGit(repo, "config", "user.email", "agent@example.test");
    }

    private void commit(Path repo, String relativePath, String message) throws IOException, InterruptedException {
        runGit(repo, "add", relativePath);
        runGit(repo, "commit", "-m", message);
    }

    private void runGit(Path repo, String... arguments) throws IOException, InterruptedException {
        var command = new String[arguments.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = repo.toString();
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        var process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var exitCode = process.waitFor();
        assertEquals(0, exitCode, output);
    }
}
