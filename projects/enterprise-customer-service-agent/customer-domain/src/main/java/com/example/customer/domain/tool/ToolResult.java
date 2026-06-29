package com.example.customer.domain.tool;

import com.example.customer.domain.support.DomainText;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 工具执行结果。
 * <p>
 * 统一表达工具成功 payload 与失败错误码，便于 Agent、调试台和 trace 共享同一语义。
 *
 * @param toolName 工具名称
 * @param status 执行状态
 * @param payload 成功结果
 * @param errorCode 失败错误码
 * @param errorMessage 失败错误信息
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public record ToolResult(
        String toolName,
        ToolResultStatus status,
        Map<String, Object> payload,
        Optional<String> errorCode,
        Optional<String> errorMessage) {

    /**
     * 创建成功工具结果。
     *
     * @param toolName 工具名称
     * @param payload 成功结果
     * @return 成功工具结果
     */
    public static ToolResult succeeded(String toolName, Map<String, Object> payload) {
        return new ToolResult(
                toolName,
                ToolResultStatus.SUCCEEDED,
                payload,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * 创建失败工具结果。
     *
     * @param toolName 工具名称
     * @param errorCode 失败错误码
     * @param errorMessage 失败错误信息
     * @return 失败工具结果
     */
    public static ToolResult failed(String toolName, String errorCode, String errorMessage) {
        return new ToolResult(
                toolName,
                ToolResultStatus.FAILED,
                Map.of(),
                Optional.of(DomainText.requireNonBlank(errorCode, "error code")),
                Optional.of(DomainText.requireNonBlank(errorMessage, "error message")));
    }

    /**
     * 判断工具是否执行成功。
     *
     * @return 成功返回 true
     */
    public boolean succeeded() {
        return status == ToolResultStatus.SUCCEEDED;
    }

    public ToolResult {
        toolName = DomainText.requireNonBlank(toolName, "tool name");
        Objects.requireNonNull(status, "tool result status must not be null");
        payload = Map.copyOf(Objects.requireNonNull(payload, "tool result payload must not be null"));
        errorCode = Objects.requireNonNull(errorCode, "tool result error code must not be null");
        errorMessage = Objects.requireNonNull(errorMessage, "tool result error message must not be null");
        if (status == ToolResultStatus.SUCCEEDED && (errorCode.isPresent() || errorMessage.isPresent())) {
            throw new IllegalArgumentException("succeeded tool result must not contain error");
        }
        if (status == ToolResultStatus.FAILED && (errorCode.isEmpty() || errorMessage.isEmpty())) {
            throw new IllegalArgumentException("failed tool result must contain error code and message");
        }
    }
}
