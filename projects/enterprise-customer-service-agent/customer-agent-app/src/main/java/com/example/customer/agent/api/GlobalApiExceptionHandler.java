package com.example.customer.agent.api;

import com.example.customer.agent.order.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 基础 API 异常处理器。
 * <p>
 * Day 04 只处理订单不存在场景，Day 05 再统一扩展参数校验和通用错误结构。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /**
     * 转换订单不存在异常。
     *
     * @param exception 订单不存在异常
     * @return 404 错误响应
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException exception) {
        var body = new ApiErrorResponse("ORDER_NOT_FOUND", "订单不存在：" + exception.orderId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
