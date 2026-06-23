package io.github.jiangzhibin.agentlearning.tool;

import java.util.List;

/**
 * 工具执行结果。
 * <p>
 * 该对象统一表达工具成功证据和失败语义，供后续 Agent 编排层消费。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public record ToolResult(
    ToolResultStatus status,
    String summary,
    List<ToolEvidence> evidence,
    String errorCode,
    String errorMessage
) {

    /**
     * 校验并复制工具执行结果。
     */
    public ToolResult {
        if (status == null) {
            throw new IllegalArgumentException("工具结果状态不能为空");
        }
        requireText(summary, "工具结果摘要");
        if (evidence == null) {
            throw new IllegalArgumentException("工具结果证据不能为空");
        }
        evidence = List.copyOf(evidence);
        if (status == ToolResultStatus.SUCCESS) {
            if (evidence.isEmpty()) {
                throw new IllegalArgumentException("成功工具结果必须包含证据");
            }
            errorCode = null;
            errorMessage = null;
        } else {
            if (!evidence.isEmpty()) {
                throw new IllegalArgumentException("失败工具结果不能包含证据");
            }
            requireText(errorCode, "工具错误码");
            requireText(errorMessage, "工具错误信息");
        }
    }

    /**
     * 创建成功工具结果。
     *
     * @param summary 结果摘要
     * @param evidence 证据列表
     * @return 工具结果
     */
    public static ToolResult success(String summary, List<ToolEvidence> evidence) {
        return new ToolResult(ToolResultStatus.SUCCESS, summary, evidence, null, null);
    }

    /**
     * 创建参数非法结果。
     *
     * @param errorMessage 错误信息
     * @return 工具结果
     */
    public static ToolResult invalidArguments(String errorMessage) {
        return failure(ToolResultStatus.INVALID_ARGUMENTS, "INVALID_ARGUMENTS", errorMessage);
    }

    /**
     * 创建权限拒绝结果。
     *
     * @param errorMessage 错误信息
     * @return 工具结果
     */
    public static ToolResult permissionDenied(String errorMessage) {
        return failure(ToolResultStatus.PERMISSION_DENIED, "PERMISSION_DENIED", errorMessage);
    }

    /**
     * 创建执行失败结果。
     *
     * @param errorMessage 错误信息
     * @return 工具结果
     */
    public static ToolResult executionFailed(String errorMessage) {
        return failure(ToolResultStatus.EXECUTION_FAILED, "EXECUTION_FAILED", errorMessage);
    }

    private static ToolResult failure(ToolResultStatus status, String errorCode, String errorMessage) {
        return new ToolResult(status, errorMessage, List.of(), errorCode, errorMessage);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }
}
