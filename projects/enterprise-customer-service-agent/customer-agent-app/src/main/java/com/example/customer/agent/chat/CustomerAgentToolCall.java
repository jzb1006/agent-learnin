package com.example.customer.agent.chat;

import java.util.Map;
import java.util.Objects;

/**
 * 客服 Agent 工具调用摘要。
 * <p>
 * 用于在 API 响应和调试台中展示工具名、参数、风险、状态、耗时和结果摘要。
 *
 * @param name 工具名称
 * @param arguments 工具参数
 * @param status 调用状态
 * @param riskLevel 风险级别
 * @param durationMs 调用耗时毫秒数
 * @param resultSummary 结果摘要
 * @author jiangzhibin
 * @since 2026-06-29 15:38:00
 */
public record CustomerAgentToolCall(
        String name,
        Map<String, String> arguments,
        String status,
        String riskLevel,
        long durationMs,
        String resultSummary) {

    /**
     * 创建工具调用摘要。
     */
    public CustomerAgentToolCall {
        Objects.requireNonNull(name, "tool call name must not be null");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "tool call arguments must not be null"));
        Objects.requireNonNull(status, "tool call status must not be null");
        Objects.requireNonNull(riskLevel, "tool call risk level must not be null");
        if (durationMs < 0) {
            throw new IllegalArgumentException("tool call duration must not be negative");
        }
        Objects.requireNonNull(resultSummary, "tool call result summary must not be null");
    }
}
