package com.example.customer.domain.tenant;

import com.example.customer.domain.support.DomainText;
import java.util.Objects;

/**
 * 客服平台租户。
 * <p>
 * 租户是订单、知识库、审批和 trace 的隔离边界。Day 03 只定义领域身份和启停状态，
 * 不绑定数据库或安全框架。
 *
 * @param id 租户唯一标识
 * @param name 租户名称
 * @param status 租户状态
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record Tenant(String id, String name, TenantStatus status) {

    /**
     * 创建启用状态租户。
     *
     * @param id 租户唯一标识
     * @param name 租户名称
     * @return 启用状态租户
     */
    public static Tenant active(String id, String name) {
        return new Tenant(id, name, TenantStatus.ACTIVE);
    }

    public Tenant {
        id = DomainText.requireNonBlank(id, "tenant id");
        name = DomainText.requireNonBlank(name, "tenant name");
        Objects.requireNonNull(status, "tenant status must not be null");
    }

    /**
     * 判断租户是否处于启用状态。
     *
     * @return 启用状态返回 true
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
