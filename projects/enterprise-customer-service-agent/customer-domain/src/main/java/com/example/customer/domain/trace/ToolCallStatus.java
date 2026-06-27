package com.example.customer.domain.trace;

/**
 * 工具调用状态。
 * <p>
 * 用于 trace 中记录工具调用是否成功。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum ToolCallStatus {

    /**
     * 调用成功。
     */
    SUCCEEDED,

    /**
     * 调用失败。
     */
    FAILED
}
