package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MCP 版只读 Git 历史查询工具。
 * <p>
 * 该工具只查询允许根目录自身的 Git 仓库历史，返回裁剪且已脱敏的提交证据。
 *
 * @author jiangzhibin
 * @since 2026-06-24 11:48:00
 */
final class GitHistoryMcpTool {

    static final String TOOL_NAME = "git_history";
    private static final String KEYWORD_ARGUMENT = "keyword";
    private static final String MAX_RESULTS_ARGUMENT = "maxResults";
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int HARD_MAX_RESULTS = 10;
    private static final int MAX_KEYWORD_LENGTH = 128;
    private static final String FIELD_SEPARATOR = "\u001f";

    private final Path allowedRoot;

    /**
     * 创建 MCP Git 历史工具。
     *
     * @param allowedRoot 允许查询 Git 仓库的根目录
     */
    GitHistoryMcpTool(Path allowedRoot) {
        if (allowedRoot == null) {
            throw new IllegalArgumentException("允许根目录不能为空");
        }
        try {
            this.allowedRoot = allowedRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("允许根目录不存在：" + allowedRoot, exception);
        }
        if (!Files.isDirectory(this.allowedRoot)) {
            throw new IllegalArgumentException("允许根目录必须是目录：" + this.allowedRoot);
        }
    }

    /**
     * 创建 git_history 工具规格。
     *
     * @return MCP 工具规格
     */
    McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(createTool())
            .callHandler((exchange, request) -> handle(request))
            .build();
    }

    private McpSchema.Tool createTool() {
        return McpSchema.Tool.builder(TOOL_NAME)
            .description("查询允许根目录自身 Git 仓库的提交历史。可按提交信息关键词过滤，最多返回 10 条。")
            .inputSchema(McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                    KEYWORD_ARGUMENT,
                    Map.of(
                        "type", "string",
                        "description", "按提交信息关键词过滤，可为空，长度不超过 128 个字符",
                        "maxLength", MAX_KEYWORD_LENGTH
                    ),
                    MAX_RESULTS_ARGUMENT,
                    Map.of(
                        "type", "integer",
                        "description", "最多返回提交数量，范围 1-10，默认 5",
                        "minimum", 1,
                        "maximum", HARD_MAX_RESULTS,
                        "default", DEFAULT_MAX_RESULTS
                    )
                ))
                .required(List.of())
                .additionalProperties(false)
                .build())
            .annotations(McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .openWorldHint(false)
                .build())
            .build();
    }

    private McpSchema.CallToolResult handle(McpSchema.CallToolRequest request) {
        if (!TOOL_NAME.equals(request.name())) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "工具名称必须是 git_history");
        }
        var arguments = request.arguments() == null ? Map.<String, Object>of() : request.arguments();
        if (hasUnknownArguments(arguments)) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "git_history 只支持 keyword 和 maxResults 参数");
        }
        var keyword = parseKeyword(arguments.get(KEYWORD_ARGUMENT));
        if (keyword == null) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "keyword 必须是字符串");
        }
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "keyword 长度不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        var maxResults = parseMaxResults(arguments.get(MAX_RESULTS_ARGUMENT));
        if (maxResults <= 0) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "maxResults 必须是 1-" + HARD_MAX_RESULTS + " 的整数");
        }
        if (!Files.isDirectory(allowedRoot.resolve(".git"))) {
            return McpToolResults.failure(McpToolResults.PERMISSION_DENIED, "Git 仓库必须位于允许根目录自身，不能向上查找允许根目录外的仓库");
        }

        try {
            var evidence = queryHistory(keyword, maxResults);
            if (evidence.isEmpty()) {
                return McpToolResults.success(
                    "未找到匹配提交",
                    List.of(Map.of("source", TOOL_NAME, "content", "未找到匹配提交"))
                );
            }
            return McpToolResults.success("返回 " + evidence.size() + " 条 Git 提交证据", evidence);
        } catch (IOException exception) {
            return McpToolResults.failure(McpToolResults.EXECUTION_FAILED, "Git 历史查询失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return McpToolResults.failure(McpToolResults.EXECUTION_FAILED, "Git 历史查询被中断：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, Object> arguments) {
        return arguments.keySet().stream()
            .anyMatch(name -> !KEYWORD_ARGUMENT.equals(name) && !MAX_RESULTS_ARGUMENT.equals(name));
    }

    private String parseKeyword(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String keyword) {
            return keyword.strip();
        }
        return null;
    }

    private int parseMaxResults(Object value) {
        if (value == null) {
            return DEFAULT_MAX_RESULTS;
        }
        if (value instanceof Number number) {
            return validMaxResults(number.intValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return validMaxResults(Integer.parseInt(text));
            } catch (NumberFormatException exception) {
                return -1;
            }
        }
        return -1;
    }

    private int validMaxResults(int value) {
        return value >= 1 && value <= HARD_MAX_RESULTS ? value : -1;
    }

    private List<Map<String, String>> queryHistory(String keyword, int maxResults) throws IOException, InterruptedException {
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
        var completed = process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
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

    private Map<String, String> toEvidence(String line) {
        var parts = line.split(FIELD_SEPARATOR, 4);
        if (parts.length < 4) {
            return Map.of("source", "commit:unknown", "content", SensitiveValueRedactor.redact(line));
        }
        var content = "date=" + parts[1]
            + " author=" + parts[2]
            + " subject=" + SensitiveValueRedactor.redact(parts[3]);
        return Map.of("source", "commit:" + parts[0], "content", content);
    }

}
