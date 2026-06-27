package com.example.customer.agent.observability;

import java.util.Optional;
import org.slf4j.MDC;

/**
 * 请求追踪上下文。
 * <p>
 * 统一封装 MDC key，避免业务层散落字符串常量。
 *
 * @author jiangzhibin
 * @since 2026-06-27 13:35:00
 */
public final class RequestTraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";

    private RequestTraceContext() {
    }

    /**
     * 获取当前请求的 traceId。
     *
     * @return 当前 traceId
     */
    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID)).filter(value -> !value.isBlank());
    }

    /**
     * 获取当前请求的 traceId，没有请求上下文时返回 fallback。
     *
     * @param fallback fallback traceId
     * @return 当前 traceId 或 fallback
     */
    public static String currentTraceIdOr(String fallback) {
        return currentTraceId().orElse(fallback);
    }
}
