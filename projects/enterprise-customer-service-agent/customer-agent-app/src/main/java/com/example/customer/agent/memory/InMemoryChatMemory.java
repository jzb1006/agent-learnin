package com.example.customer.agent.memory;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 进程内短期客服会话记忆。
 * <p>
 * 仅用于本地开发和测试；生产多实例环境应使用 Redis 存储。
 *
 * @author jiangzhibin
 * @since 2026-07-01 16:20:00
 */
@Component("customerConversationMemory")
@ConditionalOnProperty(
        prefix = "customer-agent.conversation-memory",
        name = "storage",
        havingValue = "in-memory",
        matchIfMissing = true)
public class InMemoryChatMemory implements ChatMemory {

    private final ConversationSummaryCompressor compressor;
    private final Map<MemoryKey, MutableMemory> memories = new ConcurrentHashMap<>();

    /**
     * 创建进程内客服会话记忆。
     *
     * @param properties 客服 Agent 配置
     * @param compressor 会话摘要压缩器
     */
    public InMemoryChatMemory(CustomerAgentProperties properties, ConversationSummaryCompressor compressor) {
        this.compressor = compressor;
    }

    @Override
    public ChatMemorySnapshot snapshot(String tenantId, String requestedConversationId) {
        var conversationId = normalizeConversationId(requestedConversationId);
        var memory = memories.computeIfAbsent(new MemoryKey(tenantId, conversationId), key -> new MutableMemory());
        synchronized (memory) {
            return memory.snapshot(conversationId);
        }
    }

    @Override
    public ChatMemorySnapshot remember(
            String tenantId,
            String conversationId,
            String userMessage,
            ConversationRoute route,
            String orderId) {
        var normalizedConversationId = normalizeConversationId(conversationId);
        var memory = memories.computeIfAbsent(new MemoryKey(tenantId, normalizedConversationId), key -> new MutableMemory());
        synchronized (memory) {
            memory.remember(userMessage, route, orderId, compressor);
            return memory.snapshot(normalizedConversationId);
        }
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "conversation-" + UUID.randomUUID();
        }
        return conversationId.strip();
    }

    private record MemoryKey(String tenantId, String conversationId) {
    }

    private static final class MutableMemory {

        private String summary = "";
        private String lastOrderId;

        private ChatMemorySnapshot snapshot(String conversationId) {
            return new ChatMemorySnapshot(conversationId, summary, lastOrderId);
        }

        private void remember(
                String userMessage,
                ConversationRoute route,
                String orderId,
                ConversationSummaryCompressor compressor) {
            if (orderId != null && !orderId.isBlank()) {
                lastOrderId = orderId;
            }
            summary = compressor.append(summary, lastOrderId, userMessage, route);
        }
    }
}
