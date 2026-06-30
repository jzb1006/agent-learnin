package com.example.customer.agent.observability;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.tenant.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求日志过滤器。
 * <p>
 * 为每个 HTTP 请求建立 MDC 上下文，并在请求结束后输出一条可检索的访问日志。
 *
 * @author jiangzhibin
 * @since 2026-06-27 13:35:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final int MAX_CONTEXT_VALUE_LENGTH = 128;
    private static final Pattern SAFE_CONTEXT_VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final CustomerAgentProperties properties;

    /**
     * 建立请求日志上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var startedAt = Instant.now();
        var traceId = safeHeaderValue(request.getHeader(TRACE_ID_HEADER))
                .orElseGet(() -> properties.getTraceIdPrefix() + "-" + UUID.randomUUID());
        var requestId = safeHeaderValue(request.getHeader(REQUEST_ID_HEADER)).orElseGet(() -> UUID.randomUUID().toString());
        var tenantId = safeHeaderValue(request.getHeader(TenantResolver.TENANT_ID_HEADER)).orElse("-");

        MDC.put(RequestTraceContext.TRACE_ID, traceId);
        MDC.put(RequestTraceContext.REQUEST_ID, requestId);
        MDC.put(RequestTraceContext.TENANT_ID, tenantId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            var durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                    "http_request method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
            MDC.remove(RequestTraceContext.TRACE_ID);
            MDC.remove(RequestTraceContext.REQUEST_ID);
            MDC.remove(RequestTraceContext.TENANT_ID);
        }
    }

    private static Optional<String> safeHeaderValue(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(candidate -> candidate.length() <= MAX_CONTEXT_VALUE_LENGTH)
                .filter(candidate -> SAFE_CONTEXT_VALUE.matcher(candidate).matches());
    }
}
