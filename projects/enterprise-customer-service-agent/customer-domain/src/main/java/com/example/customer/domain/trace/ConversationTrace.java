package com.example.customer.domain.trace;

import com.example.customer.domain.support.DomainText;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 对话 trace。
 * <p>
 * 保存单次客服请求的租户、原始问题、路由结果和工具调用链。该对象不可变，
 * 追加工具记录时返回新的 trace 实例。
 *
 * @param id trace 唯一标识
 * @param tenantId 归属租户标识
 * @param userMessage 用户原始消息
 * @param route 对话路由
 * @param startedAt 开始时间
 * @param toolCalls 工具调用链
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record ConversationTrace(
        String id,
        String tenantId,
        String userMessage,
        ConversationRoute route,
        Instant startedAt,
        List<ToolCallRecord> toolCalls) {

    /**
     * 创建初始对话 trace。
     *
     * @param id trace 唯一标识
     * @param tenantId 归属租户标识
     * @param userMessage 用户原始消息
     * @param route 对话路由
     * @param startedAt 开始时间
     * @return 初始对话 trace
     */
    public static ConversationTrace started(
            String id,
            String tenantId,
            String userMessage,
            ConversationRoute route,
            Instant startedAt) {
        return new ConversationTrace(id, tenantId, userMessage, route, startedAt, List.of());
    }

    public ConversationTrace {
        id = DomainText.requireNonBlank(id, "trace id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        userMessage = DomainText.requireNonBlank(userMessage, "user message");
        Objects.requireNonNull(route, "conversation route must not be null");
        Objects.requireNonNull(startedAt, "trace started at must not be null");
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "tool calls must not be null"));
    }

    /**
     * 追加工具调用记录。
     *
     * @param toolCall 工具调用记录
     * @return 包含新工具调用记录的 trace
     */
    public ConversationTrace appendToolCall(ToolCallRecord toolCall) {
        Objects.requireNonNull(toolCall, "tool call must not be null");
        var updatedToolCalls = new ArrayList<>(toolCalls);
        updatedToolCalls.add(toolCall);
        return new ConversationTrace(id, tenantId, userMessage, route, startedAt, updatedToolCalls);
    }
}
