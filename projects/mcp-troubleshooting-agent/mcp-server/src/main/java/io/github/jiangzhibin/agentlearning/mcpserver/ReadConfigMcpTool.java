package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * MCP 版只读配置读取工具。
 * <p>
 * 该工具只读取允许根目录内的常见配置文件，并在返回前脱敏敏感配置值。
 *
 * @author jiangzhibin
 * @since 2026-06-24 11:48:00
 */
final class ReadConfigMcpTool {

    static final String TOOL_NAME = "read_config";
    private static final String PATH_ARGUMENT = "path";
    private static final int MAX_LINES = 120;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".properties",
        ".yml",
        ".yaml",
        ".env",
        ".conf"
    );

    private final Path allowedRoot;

    /**
     * 创建 MCP 配置读取工具。
     *
     * @param allowedRoot 允许读取配置文件的根目录
     */
    ReadConfigMcpTool(Path allowedRoot) {
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
     * 创建 read_config 工具规格。
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
            .description("读取允许根目录内的配置文件并脱敏敏感值。仅支持 properties、yml、yaml、env、conf。")
            .inputSchema(McpSchema.JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                    PATH_ARGUMENT,
                    Map.of(
                        "type", "string",
                        "description", "允许根目录内的配置文件相对路径",
                        "minLength", 1
                    )
                ))
                .required(List.of(PATH_ARGUMENT))
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
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "工具名称必须是 read_config");
        }
        var arguments = request.arguments() == null ? Map.<String, Object>of() : request.arguments();
        if (hasUnknownArguments(arguments)) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "read_config 只支持 path 参数");
        }
        var relativePath = arguments.get(PATH_ARGUMENT);
        if (!(relativePath instanceof String path) || path.isBlank()) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "path 不能为空");
        }
        var normalizedPath = path.strip();
        if (Path.of(normalizedPath).isAbsolute()) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "path 必须是允许根目录内的相对路径");
        }
        if (!isSupportedConfigPath(normalizedPath)) {
            return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "path 必须指向支持的配置文件类型：" + SUPPORTED_EXTENSIONS);
        }

        try {
            var configFile = allowedRoot.resolve(normalizedPath).normalize();
            if (!configFile.startsWith(allowedRoot)) {
                return McpToolResults.failure(McpToolResults.PERMISSION_DENIED, "path 不能越过允许根目录");
            }
            if (Files.isSymbolicLink(configFile)) {
                return McpToolResults.failure(McpToolResults.PERMISSION_DENIED, "不允许通过符号链接读取配置文件");
            }
            if (!Files.isRegularFile(configFile, LinkOption.NOFOLLOW_LINKS)) {
                return McpToolResults.failure(McpToolResults.INVALID_ARGUMENTS, "配置文件不存在或不是普通文件");
            }
            var realPath = configFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realPath.startsWith(allowedRoot)) {
                return McpToolResults.failure(McpToolResults.PERMISSION_DENIED, "配置文件真实路径不能越过允许根目录");
            }
            var evidence = readConfigEvidence(realPath);
            if (evidence.isEmpty()) {
                return McpToolResults.success("配置文件为空", List.of(Map.of("source", source(realPath, 1), "content", "配置文件为空")));
            }
            return McpToolResults.success("读取配置文件，返回 " + evidence.size() + " 行脱敏内容", evidence);
        } catch (IOException exception) {
            return McpToolResults.failure(McpToolResults.EXECUTION_FAILED, "配置读取失败：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, Object> arguments) {
        return arguments.keySet().stream().anyMatch(name -> !PATH_ARGUMENT.equals(name));
    }

    private boolean isSupportedConfigPath(String path) {
        var lowerPath = path.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    private List<Map<String, String>> readConfigEvidence(Path file) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return IntStream.range(0, Math.min(lines.size(), MAX_LINES))
            .mapToObj(index -> Map.of(
                "source", source(file, index + 1),
                "content", SensitiveValueRedactor.redact(lines.get(index).strip())
            ))
            .toList();
    }

    private String source(Path file, int lineNumber) {
        return allowedRoot.relativize(file).toString() + ":" + lineNumber;
    }

}
