package com.example.customer.agent.api;

import java.time.Instant;

/**
 * API 错误响应。
 * <p>
 * Day 05 统一收敛错误结构，便于前端调试台、日志和后续 trace 关联。
 *
 * @param timestamp 错误发生时间
 * @param status HTTP 状态码
 * @param errorCode 错误码
 * @param message 错误消息
 * @param path 请求路径
 * @param traceId 错误追踪标识
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId) {
}
