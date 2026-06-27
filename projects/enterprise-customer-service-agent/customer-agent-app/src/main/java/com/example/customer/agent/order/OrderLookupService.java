package com.example.customer.agent.order;

import com.example.customer.domain.order.CustomerOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单查询服务。
 * <p>
 * 应用服务层统一封装订单读取和不存在时的业务异常。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@Service
@RequiredArgsConstructor
public class OrderLookupService {

    private final MockOrderRepository orderRepository;

    /**
     * 获取订单。
     *
     * @param orderId 订单标识
     * @return 客户订单
     * @throws OrderNotFoundException 订单不存在时抛出
     */
    public CustomerOrder getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
