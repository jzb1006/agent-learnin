package com.example.customer.agent.chat;

import java.util.List;

/**
 * 客服 Agent 结构化响应。
 * <p>
 * 统一承载模型或本地 fallback 生成的客服回复，便于 API、调试台和后续 trace 复用同一契约。
 *
 * @param route 路由结果
 * @param answer 客服回复正文
 * @param sources 回复依据来源
 * @param riskLevel 工具风险级别
 * @param nextActions 下一步动作
 * @param traceId trace 标识
 * @param toolCalls 工具调用摘要
 * @param conversationId 会话标识
 * @param memorySummary 会话压缩摘要
 * @param executionTrace Agent 执行链路 trace
 * @author jiangzhibin
 * @since 2026-06-27 16:05:00
 */
public record CustomerAgentResponse(
        String route,
        String answer,
        List<String> sources,
        String riskLevel,
        List<String> nextActions,
        String traceId,
        List<CustomerAgentToolCall> toolCalls,
        String conversationId,
        String memorySummary,
        CustomerAgentExecutionTrace executionTrace) {

    /**
     * 创建无工具调用的客服 Agent 结构化响应。
     *
     * @param route 路由结果
     * @param answer 客服回复正文
     * @param sources 回复依据来源
     * @param riskLevel 工具风险级别
     * @param nextActions 下一步动作
     * @param traceId trace 标识
     */
    public CustomerAgentResponse(
            String route,
            String answer,
            List<String> sources,
            String riskLevel,
            List<String> nextActions,
            String traceId) {
        this(route, answer, sources, riskLevel, nextActions, traceId, List.of(), "", "", CustomerAgentExecutionTrace.empty());
    }

    /**
     * 创建带工具调用的客服 Agent 结构化响应。
     *
     * @param route 路由结果
     * @param answer 客服回复正文
     * @param sources 回复依据来源
     * @param riskLevel 工具风险级别
     * @param nextActions 下一步动作
     * @param traceId trace 标识
     * @param toolCalls 工具调用摘要
     */
    public CustomerAgentResponse(
            String route,
            String answer,
            List<String> sources,
            String riskLevel,
            List<String> nextActions,
            String traceId,
            List<CustomerAgentToolCall> toolCalls) {
        this(route, answer, sources, riskLevel, nextActions, traceId, toolCalls, "", "", CustomerAgentExecutionTrace.empty());
    }

    /**
     * 创建客服 Agent 结构化响应。
     */
    public CustomerAgentResponse {
        sources = List.copyOf(sources);
        nextActions = List.copyOf(nextActions);
        toolCalls = List.copyOf(toolCalls);
        conversationId = conversationId == null ? "" : conversationId;
        memorySummary = memorySummary == null ? "" : memorySummary;
        executionTrace = executionTrace == null ? CustomerAgentExecutionTrace.empty() : executionTrace;
    }
}
