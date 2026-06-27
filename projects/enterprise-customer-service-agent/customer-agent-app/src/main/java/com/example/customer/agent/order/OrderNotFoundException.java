package com.example.customer.agent.order;

/**
 * 订单不存在异常。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    /**
     * 创建订单不存在异常。
     *
     * @param orderId 订单标识
     */
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }

    /**
     * 返回订单标识。
     *
     * @return 订单标识
     */
    public String orderId() {
        return orderId;
    }
}
