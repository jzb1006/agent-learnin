package com.example.customer.agent.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户 Web MVC 配置。
 * <p>
 * 将租户拦截器限定在业务与管理接口上，健康检查保持匿名可访问。
 *
 * @author jiangzhibin
 * @since 2026-06-30 11:45:00
 */
@Configuration
@RequiredArgsConstructor
public class TenantWebMvcConfiguration implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    /**
     * 注册租户上下文拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/chat", "/api/**", "/admin/**");
    }
}
