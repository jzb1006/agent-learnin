package com.example.customer.domain.tool;

/**
 * 工具参数类型。
 * <p>
 * 用于描述工具参数 schema 的基础类型，后续可映射到 Spring AI Tool Calling 或 MCP tool input schema。
 *
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public enum ToolParameterType {

    /**
     * 字符串参数。
     */
    STRING,

    /**
     * 数值参数。
     */
    NUMBER,

    /**
     * 布尔参数。
     */
    BOOLEAN,

    /**
     * 对象参数。
     */
    OBJECT,

    /**
     * 数组参数。
     */
    ARRAY
}
