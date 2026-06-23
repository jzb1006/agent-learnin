package io.github.jiangzhibin.agentlearning.tool;

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
 * 本地只读配置读取工具。
 * <p>
 * 该工具只读取允许根目录内的常见配置文件，并在返回前脱敏敏感配置值。
 *
 * @author jiangzhibin
 * @since 2026-06-23 17:26:31
 */
public record LocalConfigReadTool(Path allowedRoot) implements TroubleshootingTool {

    private static final String TOOL_NAME = "read_config";
    private static final String PATH_ARGUMENT = "path";
    private static final int MAX_LINES = 120;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".properties",
        ".yml",
        ".yaml",
        ".env",
        ".conf"
    );

    /**
     * 校验并规范化允许访问根目录。
     */
    public LocalConfigReadTool {
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
            "读取允许根目录内的配置文件并脱敏敏感值",
            List.of(ToolParameter.required(PATH_ARGUMENT, ToolParameterType.STRING, "允许根目录内的配置文件相对路径"))
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        if (!TOOL_NAME.equals(call.toolName())) {
            return ToolResult.invalidArguments("工具名称必须是 read_config");
        }
        if (hasUnknownArguments(call.arguments())) {
            return ToolResult.invalidArguments("read_config 只支持 path 参数");
        }
        var relativePath = call.arguments().get(PATH_ARGUMENT);
        if (relativePath == null || relativePath.isBlank()) {
            return ToolResult.invalidArguments("path 不能为空");
        }
        if (Path.of(relativePath.strip()).isAbsolute()) {
            return ToolResult.invalidArguments("path 必须是允许根目录内的相对路径");
        }
        if (!isSupportedConfigPath(relativePath.strip())) {
            return ToolResult.invalidArguments("path 必须指向支持的配置文件类型：" + SUPPORTED_EXTENSIONS);
        }

        try {
            var configFile = allowedRoot.resolve(relativePath.strip()).normalize();
            if (!configFile.startsWith(allowedRoot)) {
                return ToolResult.permissionDenied("path 不能越过允许根目录");
            }
            if (Files.isSymbolicLink(configFile)) {
                return ToolResult.permissionDenied("不允许通过符号链接读取配置文件");
            }
            if (!Files.isRegularFile(configFile, LinkOption.NOFOLLOW_LINKS)) {
                return ToolResult.invalidArguments("配置文件不存在或不是普通文件");
            }
            var realPath = configFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realPath.startsWith(allowedRoot)) {
                return ToolResult.permissionDenied("配置文件真实路径不能越过允许根目录");
            }
            var evidence = readConfigEvidence(realPath);
            if (evidence.isEmpty()) {
                return ToolResult.success("配置文件为空", List.of(new ToolEvidence(source(realPath, 1), "配置文件为空")));
            }
            return ToolResult.success("读取配置文件，返回 " + evidence.size() + " 行脱敏内容", evidence);
        } catch (IOException exception) {
            return ToolResult.executionFailed("配置读取失败：" + exception.getMessage());
        }
    }

    private boolean hasUnknownArguments(Map<String, String> arguments) {
        return arguments.keySet().stream().anyMatch(name -> !PATH_ARGUMENT.equals(name));
    }

    private boolean isSupportedConfigPath(String path) {
        var lowerPath = path.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    private List<ToolEvidence> readConfigEvidence(Path file) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return IntStream.range(0, Math.min(lines.size(), MAX_LINES))
            .mapToObj(index -> new ToolEvidence(source(file, index + 1), SensitiveValueRedactor.redact(lines.get(index).strip())))
            .toList();
    }

    private String source(Path file, int lineNumber) {
        return allowedRoot.relativize(file).toString() + ":" + lineNumber;
    }
}
