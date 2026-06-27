package com.example.customer.agent.order;

import com.example.customer.domain.order.CustomerOrder;
import java.time.Instant;

/**
 * 订单 API 响应。
 *
 * @param id 订单标识
 * @param tenantId 租户标识
 * @param customerId 客户标识
 * @param productName 产品名称
 * @param status 订单状态
 * @param paidAt 支付时间
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public record OrderResponse(
        String id,
        String tenantId,
        String customerId,
        String productName,
        String status,
        Instant paidAt) {

    /**
     * 从领域订单转换为 API 响应。
     *
     * @param order 领域订单
     * @return 订单响应
     */
    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.id(),
                order.tenantId(),
                order.customerId(),
                order.productName(),
                order.status().name(),
                order.paidAt());
    }
}
