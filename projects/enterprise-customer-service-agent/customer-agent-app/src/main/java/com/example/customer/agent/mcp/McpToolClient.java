package com.example.customer.agent.mcp;

import com.example.customer.domain.tool.ToolDefinition;
import java.util.List;

/**
 * MCP 工具客户端边界。
 * <p>
 * Agent App 只通过该接口发现和调用工具，生产运行时由真实 MCP transport 提供工具能力。
 *
 * @author jiangzhibin
 * @since 2026-06-30 18:24:00
 */
public interface McpToolClient extends AutoCloseable {

    /**
     * 列出可发现工具。
     *
     * @return 工具定义列表
     */
    List<ToolDefinition> listTools();

    /**
     * 调用 MCP 工具。
     *
     * @param request 工具调用请求
     * @return 工具调用响应
     */
    McpToolCallResponse call(McpToolCallRequest request);

    /**
     * 关闭 MCP client 资源。
     * <p>
     * 部分测试 double 无需释放资源；真实 stdio client 会覆盖该方法关闭子进程。
     */
    @Override
    default void close() {
    }
}
