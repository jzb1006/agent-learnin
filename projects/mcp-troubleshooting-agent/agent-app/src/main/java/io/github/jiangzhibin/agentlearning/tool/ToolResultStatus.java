package io.github.jiangzhibin.agentlearning.tool;

/**
 * 工具执行结果状态。
 * <p>
 * 状态用于区分成功、参数错误、权限拒绝和执行失败，避免把失败结果当证据使用。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public enum ToolResultStatus {

    SUCCESS,
    INVALID_ARGUMENTS,
    PERMISSION_DENIED,
    EXECUTION_FAILED
}
