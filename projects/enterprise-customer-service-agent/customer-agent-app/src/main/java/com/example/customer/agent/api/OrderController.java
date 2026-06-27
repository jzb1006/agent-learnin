package com.example.customer.agent.api;

import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.agent.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单查询 API。
 * <p>
 * 当前接口只读取 mock 订单数据，用于打通 Web 调试台和应用服务分层。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderLookupService orderLookupService;

    /**
     * 查询单个订单。
     *
     * @param orderId 订单标识
     * @return 订单响应
     */
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable("orderId") String orderId) {
        return OrderResponse.from(orderLookupService.getOrder(orderId));
    }
}
