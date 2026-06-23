package io.github.jiangzhibin.agentlearning.tool;

/**
 * 工具参数类型。
 * <p>
 * 当前只表达 Tool Calling schema 所需的基础类型，复杂对象留到真实工具需要时再扩展。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public enum ToolParameterType {

    STRING,
    INTEGER,
    BOOLEAN
}
