package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具结果构造器。
 * <p>
 * 统一只读工具的 JSON 文本返回格式，便于 Agent 侧稳定解析和测试断言。
 *
 * @author jiangzhibin
 * @since 2026-06-24 11:52:00
 */
final class McpToolResults {

    static final String SUCCESS = "SUCCESS";
    static final String INVALID_ARGUMENTS = "INVALID_ARGUMENTS";
    static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    static final String EXECUTION_FAILED = "EXECUTION_FAILED";

    private McpToolResults() {
    }

    /**
     * 构造成功工具结果。
     *
     * @param summary 结果摘要
     * @param evidence 证据列表
     * @return MCP 工具调用结果
     */
    static McpSchema.CallToolResult success(String summary, List<Map<String, String>> evidence) {
        return result(false, Map.of(
            "status", SUCCESS,
            "summary", summary,
            "evidence", evidence
        ));
    }

    /**
     * 构造失败工具结果。
     *
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return MCP 工具调用结果
     */
    static McpSchema.CallToolResult failure(String errorCode, String errorMessage) {
        return result(true, Map.of(
            "status", errorCode,
            "summary", errorMessage,
            "evidence", List.of(),
            "errorCode", errorCode,
            "errorMessage", errorMessage
        ));
    }

    private static McpSchema.CallToolResult result(boolean isError, Map<String, Object> payload) {
        try {
            return McpSchema.CallToolResult.builder()
                .addTextContent(McpJsonDefaults.getMapper().writeValueAsString(payload))
                .isError(isError)
                .build();
        } catch (IOException exception) {
            return McpSchema.CallToolResult.builder()
                .addTextContent("{\"status\":\"EXECUTION_FAILED\",\"summary\":\"工具结果序列化失败\"}")
                .isError(true)
                .build();
        }
    }
}
