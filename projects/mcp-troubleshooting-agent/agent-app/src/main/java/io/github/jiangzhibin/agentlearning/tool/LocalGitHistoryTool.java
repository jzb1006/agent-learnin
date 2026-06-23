package io.github.jiangzhibin.agentlearning.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地只读 Git 历史查询工具。
 * <p>
 * 该工具只查询允许根目录自身的 Git 仓库历史，返回裁剪后的提交证据。
 *
 * @author jiangzhibin
 * @since 2026-06-23 17:26:31
 */
public record LocalGitHistoryTool(Path allowedRoot) implements TroubleshootingTool {

    private static final String TOOL_NAME = "git_history";
    private static final String KEYWORD_ARGUMENT = "keyword";
    private static final String MAX_RESULTS_ARGUMENT = "maxResults";
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int HARD_MAX_RESULTS = 10;
    private static final int MAX_KEYWORD_LENGTH = 128;
    private static final String FIELD_SEPARATOR = "\u001f";

    /**
     * 校验并规范化允许访问根目录。
     */
    public LocalGitHistoryTool {
        if (allowedRoot == null) {
            throw new IllegalArgumentException("允许根目录不能为空");
        }
        try {
            allowedRoot = allowedRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("允许根目录不存在：" + allowedRoot, exception);
        }
        if (!Files.isDirectory(allowedRoot)) {
            throw new IllegalArgumentException("允许根目录必须是目录：" + allowedRoot);
        }
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.readOnly(
            TOOL_NAME,
            "查询允许根目录自身 Git 仓库的提交历史",
            List.of(
                ToolParameter.optional(KEYWORD_ARGUMENT, ToolParameterType.STRING, "按提交信息关键词过滤"),
                ToolParameter.optional(MAX_RESULTS_ARGUMENT, ToolParameterType.INTEGER, "最多返回提交数量，范围 1-10")
            )
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        if (!TOOL_NAME.equals(call.toolName())) {
            return ToolResult.invalidArguments("工具名称必须是 git_history");
        }
        if (hasUnknownArguments(call.arguments())) {
            return ToolResult.invalidArguments("git_history 只支持 keyword 和 maxResults 参数");
        }
        var keyword = call.arguments().get(KEYWORD_ARGUMENT);
        if (keyword != null && keyword.strip().length() > MAX_KEYWORD_LENGTH) {
            return ToolResult.invalidArguments("keyword 长度不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        var maxResults = parseMaxResults(call.arguments().get(MAX_RESULTS_ARGUMENT));
        if (maxResults <= 0) {
            return ToolResult.invalidArguments("maxResults 必须是 1-" + HARD_MAX_RESULTS + " 的整数");
        }
        if (!Files.isDirectory(allowedRoot.resolve(".git"))) {
            return ToolResult.permissionDenied("Git 仓库必须位于允许根目录自身，不能向上查找允许根目录外的仓库");
        }

        try {
            var evidence = queryHistory(keyword == null ? "" : keyword.strip(), maxResults);
            if (evidence.isEmpty()) {
                return ToolResult.success(
                    "未找到匹配提交",
                    List.of(new ToolEvidence("git_history", "未找到匹配提交"))
                );
            }
            return ToolResult.success("返回 " + evidence.size() + " 条 Git 提交证据", evidence);
        } catch (IOException exception) {
            return ToolResult.executionFailed("Git 历史查询失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ToolResult.executionFailed("Git 历史查询被中断：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, String> arguments) {
        return arguments.keySet().stream()
            .anyMatch(name -> !KEYWORD_ARGUMENT.equals(name) && !MAX_RESULTS_ARGUMENT.equals(name));
    }

    private int parseMaxResults(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MAX_RESULTS;
        }
        try {
            var parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > HARD_MAX_RESULTS) {
                return -1;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private List<ToolEvidence> queryHistory(String keyword, int maxResults) throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-C");
        command.add(allowedRoot.toString());
        command.add("log");
        command.add("-n");
        command.add(Integer.toString(maxResults));
        command.add("--date=short");
        command.add("--pretty=format:%H" + FIELD_SEPARATOR + "%ad" + FIELD_SEPARATOR + "%an" + FIELD_SEPARATOR + "%s");
        if (!keyword.isBlank()) {
            command.add("--grep=" + keyword);
        }
        var output = run(command);
        if (output.isBlank()) {
            return List.of();
        }
        return output.lines()
            .map(this::toEvidence)
            .toList();
    }

    private String run(List<String> command) throws IOException, InterruptedException {
        var process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        var completed = process.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Git 命令超时");
        }
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException(output.strip());
        }
        return output;
    }

    private ToolEvidence toEvidence(String line) {
        var parts = line.split(FIELD_SEPARATOR, 4);
        if (parts.length < 4) {
            return new ToolEvidence("commit:unknown", SensitiveValueRedactor.redact(line));
        }
        var hash = parts[0];
        var content = "date=" + parts[1]
            + " author=" + parts[2]
            + " subject=" + SensitiveValueRedactor.redact(parts[3]);
        return new ToolEvidence("commit:" + hash, content);
    }
}
