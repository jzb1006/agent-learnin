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

class LocalGitHistoryToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldQueryGitHistoryByKeyword() throws IOException, InterruptedException {
        var repo = initGitRepository();
        commit(repo, "src/main/java/App.java", "class App {}", "feat: add gateway health check");
        commit(repo, "README.md", "docs", "docs: update usage");
        var tool = new LocalGitHistoryTool(repo);

        var result = tool.execute(new ToolCall(
            "git_history",
            Map.of("keyword", "gateway", "maxResults", "3")
        ));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals(1, result.evidence().size());
        assertTrue(result.evidence().getFirst().source().startsWith("commit:"));
        assertTrue(result.evidence().getFirst().content().contains("feat: add gateway health check"));
        assertTrue(result.evidence().getFirst().content().contains("Agent Learner"));
    }

    @Test
    void shouldRedactSensitiveTextInCommitSubject() throws IOException, InterruptedException {
        var repo = initGitRepository();
        commit(repo, "application.yml", "server.port=8080", "fix: rotate password=plain-text-secret");
        var tool = new LocalGitHistoryTool(repo);

        var result = tool.execute(new ToolCall("git_history", Map.of("keyword", "password")));

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertFalse(result.evidence().getFirst().content().contains("plain-text-secret"));
        assertTrue(result.evidence().getFirst().content().contains("password=[REDACTED]"));
    }

    @Test
    void shouldRejectRepositoryAboveAllowedRoot() throws IOException, InterruptedException {
        var repo = initGitRepository();
        commit(repo, "src/main/java/App.java", "class App {}", "feat: add app");
        var srcRoot = repo.resolve("src");
        var tool = new LocalGitHistoryTool(srcRoot);

        var result = tool.execute(new ToolCall("git_history", Map.of()));

        assertEquals(ToolResultStatus.PERMISSION_DENIED, result.status());
        assertTrue(result.errorMessage().contains("允许根目录"));
    }

    @Test
    void shouldRejectInvalidMaxResults() throws IOException, InterruptedException {
        var repo = initGitRepository();
        commit(repo, "README.md", "docs", "docs: update usage");
        var tool = new LocalGitHistoryTool(repo);

        var result = tool.execute(new ToolCall("git_history", Map.of("maxResults", "0")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("maxResults"));
    }

    @Test
    void shouldRejectUnexpectedArgument() throws IOException, InterruptedException {
        var repo = initGitRepository();
        commit(repo, "README.md", "docs", "docs: update usage");
        var tool = new LocalGitHistoryTool(repo);

        var result = tool.execute(new ToolCall("git_history", Map.of("path", "application.yml")));

        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, result.status());
        assertTrue(result.errorMessage().contains("keyword"));
        assertTrue(result.errorMessage().contains("maxResults"));
    }

    private Path initGitRepository() throws IOException, InterruptedException {
        var repo = tempDir.resolve("repo");
        Files.createDirectories(repo);
        runGit(repo, "init");
        runGit(repo, "config", "user.name", "Agent Learner");
        runGit(repo, "config", "user.email", "agent@example.test");
        return repo;
    }

    private void commit(Path repo, String relativePath, String content, String message)
        throws IOException, InterruptedException {
        var file = repo.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
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
        Assumptions.assumeTrue(exitCode == 0, output);
    }
}
