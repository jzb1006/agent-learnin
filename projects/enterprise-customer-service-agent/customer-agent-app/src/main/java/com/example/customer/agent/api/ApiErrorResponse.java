package com.example.customer.agent.api;

/**
 * API 错误响应。
 * <p>
 * Day 04 只承载基础错误码和错误消息，完整错误响应结构在 Day 05 统一收敛。
 *
 * @param errorCode 错误码
 * @param message 错误消息
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ApiErrorResponse(String errorCode, String message) {
}
