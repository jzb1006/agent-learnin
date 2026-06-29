package com.example.customer.agent.chat;

/**
 * 客服 Agent 结构化响应解析异常。
 * <p>
 * 当模型返回无法映射为稳定 Java 契约时抛出，错误消息必须包含可定位字段名。
 *
 * @author jiangzhibin
 * @since 2026-06-27 16:05:00
 */
public class CustomerAgentResponseParseException extends RuntimeException {

    /**
     * 创建结构化响应解析异常。
     *
     * @param message 错误消息
     */
    public CustomerAgentResponseParseException(String message) {
        super(message);
    }

    /**
     * 创建结构化响应解析异常。
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public CustomerAgentResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
