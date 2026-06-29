package com.example.customer.agent.order;

import com.example.customer.domain.order.CustomerOrder;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * mock 订单仓库。
 * <p>
 * Day 04 使用内存数据验证 API 分层，后续再替换为 PostgreSQL 查询实现。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@Repository
public class MockOrderRepository {

    private static final Map<String, CustomerOrder> ORDERS = Map.of(
            "order-1001",
            CustomerOrder.paid(
                    "order-1001",
                    "tenant-demo",
                    "customer-1001",
                    "企业级 AI Agent 实战营",
                    Instant.parse("2026-06-01T10:00:00Z")));

    /**
     * 根据订单标识查询 mock 订单。
     *
     * @param orderId 订单标识
     * @return 订单
     */
    public Optional<CustomerOrder> findById(String orderId) {
        return Optional.ofNullable(ORDERS.get(orderId));
    }

    /**
     * 根据订单标识和租户标识查询 mock 订单。
     *
     * @param orderId 订单标识
     * @param tenantId 租户标识
     * @return 订单
     */
    public Optional<CustomerOrder> findByIdAndTenantId(String orderId, String tenantId) {
        return findById(orderId)
                .filter(order -> order.tenantId().equals(tenantId));
    }
}
