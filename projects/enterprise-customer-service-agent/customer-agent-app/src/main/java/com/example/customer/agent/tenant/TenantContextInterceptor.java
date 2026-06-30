package com.example.customer.agent.tenant;

import com.example.customer.agent.observability.RequestTraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户上下文拦截器。
 * <p>
 * 对业务接口强制解析 `X-Tenant-ID`，并在请求结束后清理线程上下文。
 *
 * @author jiangzhibin
 * @since 2026-06-30 11:45:00
 */
@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {

    private final TenantResolver tenantResolver;

    /**
     * 在进入 Controller 前建立租户上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return 是否继续处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var tenantId = tenantResolver.resolveRequiredTenantId(request);
        TenantContext.setCurrentTenantId(tenantId);
        MDC.put(RequestTraceContext.TENANT_ID, tenantId);
        return true;
    }

    /**
     * 请求完成后清理租户上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @param exception 请求异常
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        TenantContext.clear();
    }
}
