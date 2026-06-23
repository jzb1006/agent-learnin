package io.github.jiangzhibin.agentlearning.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地只读代码搜索工具。
 * <p>
 * 该工具只扫描允许根目录内的 Java 源码文件，并返回带来源的匹配片段。
 *
 * @author jiangzhibin
 * @since 2026-06-23 17:03:18
 */
public record LocalCodeSearchTool(Path allowedRoot) implements TroubleshootingTool {

    private static final String TOOL_NAME = "search_code";
    private static final String KEYWORD_ARGUMENT = "keyword";
    private static final int MAX_KEYWORD_LENGTH = 128;
    private static final int MAX_RESULTS = 5;

    /**
     * 校验并规范化允许访问根目录。
     *
     * @throws IllegalArgumentException 当根目录为空、不存在或不是目录时抛出
     */
    public LocalCodeSearchTool {
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
            "按关键词搜索允许根目录内的 Java 源码文件",
            List.of(ToolParameter.required(KEYWORD_ARGUMENT, ToolParameterType.STRING, "搜索关键词"))
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        if (!TOOL_NAME.equals(call.toolName())) {
            return ToolResult.invalidArguments("工具名称必须是 search_code");
        }
        var keyword = call.arguments().get(KEYWORD_ARGUMENT);
        if (keyword == null || keyword.isBlank()) {
            return ToolResult.invalidArguments("keyword 不能为空");
        }
        if (keyword.strip().length() > MAX_KEYWORD_LENGTH) {
            return ToolResult.invalidArguments("keyword 长度不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        if (hasUnknownArguments(call.arguments())) {
            return ToolResult.invalidArguments("search_code 只支持 keyword 参数");
        }

        try {
            var normalizedKeyword = keyword.strip();
            var evidence = search(normalizedKeyword);
            if (evidence.isEmpty()) {
                return ToolResult.success(
                    "未找到匹配片段",
                    List.of(new ToolEvidence("search_code", "未找到匹配片段：" + normalizedKeyword))
                );
            }
            var summary = evidence.size() == MAX_RESULTS
                ? "找到匹配片段，返回前 " + MAX_RESULTS + " 个"
                : "找到 " + evidence.size() + " 个匹配片段";
            return ToolResult.success(summary, evidence);
        } catch (IOException exception) {
            return ToolResult.executionFailed("代码搜索失败：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, String> arguments) {
        return arguments.keySet().stream().anyMatch(name -> !KEYWORD_ARGUMENT.equals(name));
    }

    private List<ToolEvidence> search(String keyword) throws IOException {
        var evidence = new ArrayList<ToolEvidence>();
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

    private void collectMatches(Path file, String keyword, List<ToolEvidence> evidence) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (var index = 0; index < lines.size(); index++) {
            var line = lines.get(index);
            if (line.contains(keyword)) {
                evidence.add(new ToolEvidence(source(file, index + 1), line.strip()));
                if (evidence.size() >= MAX_RESULTS) {
                    return;
                }
            }
        }
    }

    private String source(Path file, int lineNumber) {
        return allowedRoot.relativize(file).toString() + ":" + lineNumber;
    }
}
