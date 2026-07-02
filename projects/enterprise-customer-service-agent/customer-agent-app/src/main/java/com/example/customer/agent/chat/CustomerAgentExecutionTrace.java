package com.example.customer.agent.chat;

import java.util.List;
import java.util.Objects;

/**
 * 客服 Agent 执行链路 trace。
 * <p>
 * 记录单次 `/chat` 请求的路由、风险、证据、最终回复和可展示的 Agent Loop 步骤。
 *
 * @param traceId trace 标识
 * @param tenantId 租户标识
 * @param conversationId 会话标识
 * @param route 路由结果
 * @param riskLevel 风险级别
 * @param evidence 回复证据
 * @param finalAnswer 最终回复
 * @param steps Agent Loop 步骤
 * @author jiangzhibin
 * @since 2026-07-02 09:25:00
 */
public record CustomerAgentExecutionTrace(
        String traceId,
        String tenantId,
        String conversationId,
        String route,
        String riskLevel,
        List<String> evidence,
        String finalAnswer,
        List<CustomerAgentExecutionStep> steps) {

    /**
     * 创建空执行 trace。
     *
     * @return 空执行 trace
     */
    public static CustomerAgentExecutionTrace empty() {
        return new CustomerAgentExecutionTrace("", "", "", "", "", List.of(), "", List.of());
    }

    /**
     * 创建执行 trace。
     */
    public CustomerAgentExecutionTrace {
        Objects.requireNonNull(traceId, "execution trace id must not be null");
        Objects.requireNonNull(tenantId, "execution tenant id must not be null");
        Objects.requireNonNull(conversationId, "execution conversation id must not be null");
        Objects.requireNonNull(route, "execution route must not be null");
        Objects.requireNonNull(riskLevel, "execution risk level must not be null");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "execution evidence must not be null"));
        Objects.requireNonNull(finalAnswer, "execution final answer must not be null");
        steps = List.copyOf(Objects.requireNonNull(steps, "execution steps must not be null"));
    }
}
