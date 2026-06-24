package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP 版只读代码搜索工具。
 * <p>
 * 该工具只扫描允许根目录内的 Java 源码文件，并通过 MCP 文本内容返回结构化 JSON 结果。
 *
 * @author jiangzhibin
 * @since 2026-06-24 10:15:00
 */
final class SearchCodeMcpTool {

    static final String TOOL_NAME = "search_code";
    private static final String KEYWORD_ARGUMENT = "keyword";
    private static final int MAX_KEYWORD_LENGTH = 128;
    private static final int MAX_RESULTS = 5;
    private static final String SUCCESS = "SUCCESS";
    private static final String INVALID_ARGUMENTS = "INVALID_ARGUMENTS";
    private static final String EXECUTION_FAILED = "EXECUTION_FAILED";

    private final Path allowedRoot;

    /**
     * 创建 MCP 代码搜索工具。
     *
     * @param allowedRoot 允许搜索的根目录
     */
    SearchCodeMcpTool(Path allowedRoot) {
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
     * 创建 search_code 工具规格。
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
            .description("按关键词搜索允许根目录内的 Java 源码文件。只返回最多 5 个匹配片段。")
            .inputSchema(McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                    KEYWORD_ARGUMENT,
                    Map.of(
                        "type", "string",
                        "description", "搜索关键词，不能为空，长度不超过 128 个字符",
                        "minLength", 1,
                        "maxLength", MAX_KEYWORD_LENGTH
                    )
                ))
                .required(List.of(KEYWORD_ARGUMENT))
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
            return failure(INVALID_ARGUMENTS, "工具名称必须是 search_code");
        }
        var arguments = request.arguments() == null ? Map.<String, Object>of() : request.arguments();
        if (hasUnknownArguments(arguments)) {
            return failure(INVALID_ARGUMENTS, "search_code 只支持 keyword 参数");
        }
        var keywordValue = arguments.get(KEYWORD_ARGUMENT);
        if (!(keywordValue instanceof String keyword) || keyword.isBlank()) {
            return failure(INVALID_ARGUMENTS, "keyword 不能为空");
        }
        if (keyword.strip().length() > MAX_KEYWORD_LENGTH) {
            return failure(INVALID_ARGUMENTS, "keyword 长度不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }

        try {
            var normalizedKeyword = keyword.strip();
            var evidence = search(normalizedKeyword);
            if (evidence.isEmpty()) {
                return success(
                    "未找到匹配片段",
                    List.of(Map.of("source", TOOL_NAME, "content", "未找到匹配片段：" + normalizedKeyword))
                );
            }
            var summary = evidence.size() == MAX_RESULTS
                ? "找到匹配片段，返回前 " + MAX_RESULTS + " 个"
                : "找到 " + evidence.size() + " 个匹配片段";
            return success(summary, evidence);
        } catch (IOException exception) {
            return failure(EXECUTION_FAILED, "代码搜索失败：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, Object> arguments) {
        return arguments.keySet().stream().anyMatch(name -> !KEYWORD_ARGUMENT.equals(name));
    }

    private List<Map<String, String>> search(String keyword) throws IOException {
        var evidence = new ArrayList<Map<String, String>>();
        try (var paths = Files.walk(allowedRoot)) {
            var javaFiles = paths
                .filter(this::isSearchableJavaFile)
                .sorted()
                .toList();
            for (var javaFile : javaFiles) {
                collectMatches(javaFile, keyword, evidence);
                if (evidence.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return evidence;
    }

    private boolean isSearchableJavaFile(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            && !Files.isSymbolicLink(path)
            && path.getFileName().toString().endsWith(".java")
            && isInsideAllowedRoot(path);
    }

    private boolean isInsideAllowedRoot(Path path) {
        try {
            return path.toRealPath().startsWith(allowedRoot);
        } catch (IOException exception) {
            return false;
        }
    }

    private void collectMatches(Path file, String keyword, List<Map<String, String>> evidence) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (var index = 0; index < lines.size(); index++) {
            var line = lines.get(index);
            if (line.contains(keyword)) {
                evidence.add(Map.of("source", source(file, index + 1), "content", line.strip()));
                if (evidence.size() >= MAX_RESULTS) {
                    return;
                }
            }
        }
    }

    private String source(Path file, int lineNumber) {
        return allowedRoot.relativize(file).toString() + ":" + lineNumber;
    }

    private McpSchema.CallToolResult success(String summary, List<Map<String, String>> evidence) {
        return result(false, Map.of(
            "status", SUCCESS,
            "summary", summary,
            "evidence", evidence
        ));
    }

    private McpSchema.CallToolResult failure(String errorCode, String errorMessage) {
        return result(true, Map.of(
            "status", errorCode,
            "summary", errorMessage,
            "evidence", List.of(),
            "errorCode", errorCode,
            "errorMessage", errorMessage
        ));
    }

    private McpSchema.CallToolResult result(boolean isError, Map<String, Object> payload) {
        try {
            return McpSchema.CallToolResult.builder()
                .addTextContent(McpJsonDefaults.getMapper().writeValueAsString(payload))
                .isError(isError)
                .build();
        } catch (IOException exception) {
            return McpSchema.CallToolResult.builder()
                .addTextContent("{\"status\":\"EXECUTION_FAILED\",\"summary\":\"工具结果序列化失败\"}")
                .isError(true)
                .build();
        }
    }
}
