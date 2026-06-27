package com.example.customer.agent.chat;

/**
 * 模型调用业务异常。
 *
 * @author jiangzhibin
 * @since 2026-06-27 10:55:00
 */
public class ChatModelException extends RuntimeException {

    /**
     * 创建模型调用业务异常。
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ChatModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
