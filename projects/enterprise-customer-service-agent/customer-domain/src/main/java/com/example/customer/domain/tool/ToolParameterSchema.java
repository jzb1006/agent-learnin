package com.example.customer.domain.tool;

import com.example.customer.domain.support.DomainText;
import java.util.Objects;

/**
 * 工具参数 schema。
 * <p>
 * 描述单个工具参数的名称、类型、说明、是否必填以及是否包含敏感信息。
 *
 * @param name 参数名
 * @param type 参数类型
 * @param description 参数说明
 * @param required 是否必填
 * @param sensitive 是否敏感
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public record ToolParameterSchema(
        String name,
        ToolParameterType type,
        String description,
        boolean required,
        boolean sensitive) {

    /**
     * 创建必填参数。
     *
     * @param name 参数名
     * @param type 参数类型
     * @param description 参数说明
     * @return 必填参数 schema
     */
    public static ToolParameterSchema required(String name, ToolParameterType type, String description) {
        return new ToolParameterSchema(name, type, description, true, false);
    }

    /**
     * 创建可选参数。
     *
     * @param name 参数名
     * @param type 参数类型
     * @param description 参数说明
     * @return 可选参数 schema
     */
    public static ToolParameterSchema optional(String name, ToolParameterType type, String description) {
        return new ToolParameterSchema(name, type, description, false, false);
    }

    /**
     * 创建敏感参数。
     *
     * @param name 参数名
     * @param type 参数类型
     * @param description 参数说明
     * @return 敏感参数 schema
     */
    public static ToolParameterSchema sensitive(String name, ToolParameterType type, String description) {
        return new ToolParameterSchema(name, type, description, true, true);
    }

    public ToolParameterSchema {
        name = DomainText.requireNonBlank(name, "tool parameter name");
        Objects.requireNonNull(type, "tool parameter type must not be null");
        description = DomainText.requireNonBlank(description, "tool parameter description");
    }
}
