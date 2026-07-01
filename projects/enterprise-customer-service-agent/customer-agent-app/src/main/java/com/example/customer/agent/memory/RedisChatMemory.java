package com.example.customer.agent.memory;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.domain.trace.ConversationRoute;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis 短期客服会话记忆。
 * <p>
 * 以 tenantId + conversationId 作为隔离键，写入压缩摘要和最近订单号，并设置 TTL。
 *
 * @author jiangzhibin
 * @since 2026-07-01 16:20:00
 */
@Component("customerConversationMemory")
@ConditionalOnProperty(prefix = "customer-agent.conversation-memory", name = "storage", havingValue = "redis")
public class RedisChatMemory implements ChatMemory {

    private final CustomerAgentProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ConversationSummaryCompressor compressor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Redis 客服会话记忆。
     *
     * @param properties 客服 Agent 配置
     * @param redisTemplate Redis 字符串操作模板
     * @param compressor 会话摘要压缩器
     */
    public RedisChatMemory(
            CustomerAgentProperties properties,
            StringRedisTemplate redisTemplate,
            ConversationSummaryCompressor compressor) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.compressor = compressor;
    }

    @Override
    public ChatMemorySnapshot snapshot(String tenantId, String requestedConversationId) {
        var conversationId = normalizeConversationId(requestedConversationId);
        var value = redisTemplate.opsForValue().get(redisKey(tenantId, conversationId));
        if (value == null || value.isBlank()) {
            return new ChatMemorySnapshot(conversationId, "", null);
        }
        return parse(conversationId, value);
    }

    @Override
    public ChatMemorySnapshot remember(
            String tenantId,
            String conversationId,
            String userMessage,
            ConversationRoute route,
            String orderId) {
        var normalizedConversationId = normalizeConversationId(conversationId);
        var existing = snapshot(tenantId, normalizedConversationId);
        var lastOrderId = orderId == null || orderId.isBlank() ? existing.lastOrderId() : orderId;
        var summary = compressor.append(existing.summary(), lastOrderId, userMessage, route);
        var snapshot = new ChatMemorySnapshot(normalizedConversationId, summary, lastOrderId);
        redisTemplate.opsForValue().set(
                redisKey(tenantId, normalizedConversationId),
                write(snapshot),
                Duration.ofSeconds(properties.getConversationMemory().getTtlSeconds()));
        return snapshot;
    }

    private String redisKey(String tenantId, String conversationId) {
        return "%s:%s:%s".formatted(
                properties.getConversationMemory().getRedisKeyPrefix(),
                keySegment(tenantId),
                keySegment(conversationId));
    }

    private String keySegment(String value) {
        var normalized = value == null ? "" : value.strip();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "conversation-" + UUID.randomUUID();
        }
        return conversationId.strip();
    }

    private ChatMemorySnapshot parse(String fallbackConversationId, String value) {
        try {
            var node = objectMapper.readTree(value);
            return new ChatMemorySnapshot(
                    textOr(node.get("conversationId"), fallbackConversationId),
                    textOr(node.get("summary"), ""),
                    textOr(node.get("lastOrderId"), null));
        } catch (JacksonException exception) {
            return new ChatMemorySnapshot(fallbackConversationId, "", null);
        }
    }

    private String write(ChatMemorySnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Redis 会话记忆序列化失败: " + snapshot.conversationId(), exception);
        }
    }

    private String textOr(JsonNode node, String fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        return node.asString(fallback);
    }
}
