package com.example.customer.agent.api;

import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.agent.order.OrderResponse;
import com.example.customer.agent.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        var tenantId = TenantContext.requireCurrentTenantId();
        log.info("order_api_request orderId={} tenantId={}", orderId, tenantId);
        return OrderResponse.from(orderLookupService.getOrder(orderId, tenantId));
    }
}
