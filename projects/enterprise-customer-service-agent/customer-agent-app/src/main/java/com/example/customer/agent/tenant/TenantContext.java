package com.example.customer.agent.tenant;

import java.util.Optional;

/**
 * 当前请求租户上下文。
 * <p>
 * 只保存已由入口层解析和校验过的租户标识，业务代码不再直接信任客户端 body 中的租户字段。
 *
 * @author jiangzhibin
 * @since 2026-06-30 11:45:00
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * 设置当前请求租户。
     *
     * @param tenantId 租户标识
     */
    public static void setCurrentTenantId(String tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前请求租户。
     *
     * @return 当前租户标识
     */
    public static Optional<String> currentTenantId() {
        return Optional.ofNullable(CURRENT_TENANT_ID.get()).filter(value -> !value.isBlank());
    }

    /**
     * 获取当前请求租户；没有租户上下文时抛出异常。
     *
     * @return 当前租户标识
     */
    public static String requireCurrentTenantId() {
        return currentTenantId().orElseThrow(() -> TenantResolutionException.required(TenantResolver.TENANT_ID_HEADER));
    }

    /**
     * 清理当前请求租户。
     */
    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
