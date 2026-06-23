package io.github.jiangzhibin.agentlearning.tool;

import java.util.HashSet;
import java.util.List;

/**
 * 工具定义。
 * <p>
 * 该对象描述模型可见的工具 schema、权限边界和幂等性，不负责执行工具。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public record ToolDefinition(
    String name,
    String description,
    List<ToolParameter> parameters,
    boolean readOnly,
    boolean idempotent
) {

    /**
     * 校验工具定义。
     */
    public ToolDefinition {
        requireText(name, "工具名称");
        requireText(description, "工具描述");
        if (parameters == null) {
            throw new IllegalArgumentException("工具参数不能为空");
        }
        parameters = List.copyOf(parameters);
        ensureUniqueParameterNames(parameters);
    }

    /**
     * 创建只读工具定义。
     *
     * @param name 工具名称
     * @param description 工具描述
     * @param parameters 参数 schema
     * @return 只读且幂等的工具定义
     */
    public static ToolDefinition readOnly(String name, String description, List<ToolParameter> parameters) {
        return new ToolDefinition(name, description, parameters, true, true);
    }

    private static void ensureUniqueParameterNames(List<ToolParameter> parameters) {
        var names = new HashSet<String>();
        for (var parameter : parameters) {
            if (parameter == null) {
                throw new IllegalArgumentException("工具参数元素不能为空");
            }
            if (!names.add(parameter.name())) {
                throw new IllegalArgumentException("工具参数名称重复：" + parameter.name());
            }
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }
}
