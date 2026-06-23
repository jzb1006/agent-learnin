package io.github.jiangzhibin.agentlearning.tool;

/**
 * 排障工具契约。
 * <p>
 * 工具实现负责暴露定义并执行一次调用，Agent 编排层只依赖该接口。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public interface TroubleshootingTool {

    /**
     * 返回工具定义。
     *
     * @return 工具 schema 和权限元数据
     */
    ToolDefinition definition();

    /**
     * 执行工具调用。
     *
     * @param call 工具调用请求
     * @return 工具执行结果
     */
    ToolResult execute(ToolCall call);
}
