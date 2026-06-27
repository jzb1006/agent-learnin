package com.example.customer.agent.api;

import com.example.customer.agent.chat.ChatModelException;
import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.observability.RequestTraceContext;
import com.example.customer.agent.order.OrderNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
@RequiredArgsConstructor
@Slf4j
public class GlobalApiExceptionHandler {

    private final CustomerAgentProperties properties;

    /**
     * 转换订单不存在异常。
     *
     * @param exception 订单不存在异常
     * @param request HTTP 请求
     * @return 404 错误响应
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request) {
        var status = HttpStatus.NOT_FOUND;
        log.warn("api_error errorCode=ORDER_NOT_FOUND path={} orderId={}", request.getRequestURI(), exception.orderId());
        var body = error(status, "ORDER_NOT_FOUND", "订单不存在：" + exception.orderId(), request);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 转换请求参数校验异常。
     *
     * @param exception 参数校验异常
     * @param request HTTP 请求
     * @return 400 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        var status = HttpStatus.BAD_REQUEST;
        var message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        log.warn("api_error errorCode=VALIDATION_ERROR path={} message={}", request.getRequestURI(), message);
        var body = error(status, "VALIDATION_ERROR", message, request);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 转换模型调用异常。
     *
     * @param exception 模型调用异常
     * @param request HTTP 请求
     * @return 502 错误响应
     */
    @ExceptionHandler(ChatModelException.class)
    public ResponseEntity<ApiErrorResponse> handleChatModelError(
            ChatModelException exception,
            HttpServletRequest request) {
        var status = HttpStatus.BAD_GATEWAY;
        log.error("api_error errorCode=CHAT_MODEL_ERROR path={}", request.getRequestURI(), exception);
        var body = error(status, "CHAT_MODEL_ERROR", exception.getMessage(), request);
        return ResponseEntity.status(status).body(body);
    }

    private ApiErrorResponse error(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                RequestTraceContext.currentTraceIdOr(properties.getTraceIdPrefix() + "-" + UUID.randomUUID()));
    }
}
