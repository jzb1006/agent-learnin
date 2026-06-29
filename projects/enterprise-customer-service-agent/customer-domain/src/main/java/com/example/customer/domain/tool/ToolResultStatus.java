package com.example.customer.domain.tool;

/**
 * 工具执行结果状态。
 * <p>
 * 用于区分工具执行成功和可定位失败。
 *
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public enum ToolResultStatus {

    /**
     * 工具执行成功。
     */
    SUCCEEDED,

    /**
     * 工具执行失败。
     */
    FAILED
}
