package io.github.jiangzhibin.agentlearning.tool;

import java.util.Map;

/**
 * 工具调用请求。
 * <p>
 * 该对象表示模型选择某个工具后生成的调用参数。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public record ToolCall(String toolName, Map<String, String> arguments) {

    /**
     * 校验并复制工具调用参数。
     */
    public ToolCall {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("工具参数不能为空");
        }
        arguments = Map.copyOf(arguments);
    }
}
