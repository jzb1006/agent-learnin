package com.example.customer.agent.security;

/**
 * 工具权限拒绝异常。
 * <p>
 * 高风险工具在未进入人工审批边界前抛出该异常，防止模型或调用方直接执行真实写操作。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public class ToolPermissionDeniedException extends RuntimeException {

    /**
     * 创建工具权限拒绝异常。
     *
     * @param message 异常消息
     */
    public ToolPermissionDeniedException(String message) {
        super(message);
    }
}
