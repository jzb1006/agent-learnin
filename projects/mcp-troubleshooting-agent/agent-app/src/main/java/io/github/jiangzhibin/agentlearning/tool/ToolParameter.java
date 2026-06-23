package io.github.jiangzhibin.agentlearning.tool;

/**
 * 工具参数 schema。
 * <p>
 * 该对象描述模型可传入的单个参数，不承载具体工具执行逻辑。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public record ToolParameter(
    String name,
    ToolParameterType type,
    String description,
    boolean required
) {

    /**
     * 校验工具参数定义。
     */
    public ToolParameter {
        requireText(name, "参数名称");
        if (type == null) {
            throw new IllegalArgumentException("参数类型不能为空");
        }
        requireText(description, "参数描述");
    }

    /**
     * 创建必填参数。
     *
     * @param name 参数名称
     * @param type 参数类型
     * @param description 参数描述
     * @return 工具参数 schema
     */
    public static ToolParameter required(String name, ToolParameterType type, String description) {
        return new ToolParameter(name, type, description, true);
    }

    /**
     * 创建可选参数。
     *
     * @param name 参数名称
     * @param type 参数类型
     * @param description 参数描述
     * @return 工具参数 schema
     */
    public static ToolParameter optional(String name, ToolParameterType type, String description) {
        return new ToolParameter(name, type, description, false);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }
}
