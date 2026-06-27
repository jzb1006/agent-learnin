package com.example.customer.domain.order;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tenant.Tenant;
import java.time.Instant;
import java.util.Objects;

/**
 * 客户订单。
 * <p>
 * 订单属于单一租户，是客服 Agent 查询履约、课程或商品信息的只读业务对象。
 *
 * @param id 订单唯一标识
 * @param tenantId 归属租户标识
 * @param customerId 客户标识
 * @param productName 产品名称
 * @param status 订单状态
 * @param paidAt 支付时间
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record CustomerOrder(
        String id,
        String tenantId,
        String customerId,
        String productName,
        OrderStatus status,
        Instant paidAt) {

    /**
     * 创建已支付订单。
     *
     * @param id 订单唯一标识
     * @param tenantId 归属租户标识
     * @param customerId 客户标识
     * @param productName 产品名称
     * @param paidAt 支付时间
     * @return 已支付订单
     */
    public static CustomerOrder paid(
            String id,
            String tenantId,
            String customerId,
            String productName,
            Instant paidAt) {
        return new CustomerOrder(id, tenantId, customerId, productName, OrderStatus.PAID, paidAt);
    }

    public CustomerOrder {
        id = DomainText.requireNonBlank(id, "order id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        customerId = DomainText.requireNonBlank(customerId, "customer id");
        productName = DomainText.requireNonBlank(productName, "product name");
        Objects.requireNonNull(status, "order status must not be null");
        Objects.requireNonNull(paidAt, "paid at must not be null");
    }

    /**
     * 判断订单是否属于指定租户。
     *
     * @param tenant 租户
     * @return 属于该租户返回 true
     */
    public boolean belongsTo(Tenant tenant) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        return tenantId.equals(tenant.id());
    }

    /**
     * 判断订单是否可以暴露给指定租户。
     *
     * @param tenant 租户
     * @return 租户启用且订单归属匹配时返回 true
     */
    public boolean canBeExposedTo(Tenant tenant) {
        return belongsTo(tenant) && tenant.isActive();
    }
}
