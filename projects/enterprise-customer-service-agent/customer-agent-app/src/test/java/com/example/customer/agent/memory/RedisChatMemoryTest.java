package com.example.customer.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.domain.trace.ConversationRoute;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

class RedisChatMemoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> mockedValueOperations = mock(ValueOperations.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mockedValueOperations;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldPersistSnapshotWithTenantScopedRedisKeyAndTtl() {
        var properties = new CustomerAgentProperties();
        properties.getConversationMemory().setTtlSeconds(1800);
        properties.getConversationMemory().setRedisKeyPrefix("customer-agent:test:memory");
        var memory = new RedisChatMemory(properties, redisTemplate, new ConversationSummaryCompressor(properties));

        var first = memory.remember(
                "tenant-demo",
                "conversation-redis",
                "帮我查询订单 order-1001 什么时候开课",
                ConversationRoute.ORDER_LOOKUP,
                "order-1001");
        when(valueOperations.get("customer-agent:test:memory:dGVuYW50LWRlbW8:Y29udmVyc2F0aW9uLXJlZGlz"))
                .thenReturn("""
                        {"conversationId":"conversation-redis","summary":"最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课","lastOrderId":"order-1001"}
                        """);
        var second = memory.snapshot("tenant-demo", "conversation-redis");
        var isolated = memory.snapshot("tenant-other", "conversation-redis");

        assertThat(first.conversationId()).isEqualTo("conversation-redis");
        assertThat(second.summary()).contains("order-1001");
        assertThat(second.lastOrderId()).isEqualTo("order-1001");
        assertThat(isolated.summary()).isEmpty();
        verify(valueOperations).set(
                eq("customer-agent:test:memory:dGVuYW50LWRlbW8:Y29udmVyc2F0aW9uLXJlZGlz"),
                org.mockito.ArgumentMatchers.contains("\"lastOrderId\":\"order-1001\""),
                eq(Duration.ofSeconds(1800)));
    }

    @Test
    void shouldWriteValidJsonWhenConversationIdContainsControlCharacters() throws Exception {
        var properties = new CustomerAgentProperties();
        var memory = new RedisChatMemory(properties, redisTemplate, new ConversationSummaryCompressor(properties));

        memory.remember(
                "tenant-demo",
                "conversation-\"redis\none",
                "帮我查询订单 order-1001",
                ConversationRoute.ORDER_LOOKUP,
                "order-1001");

        var jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("customer-agent:conversation-memory:dGVuYW50LWRlbW8:Y29udmVyc2F0aW9uLSJyZWRpcwpvbmU"),
                jsonCaptor.capture(),
                eq(Duration.ofSeconds(7200)));
        var json = objectMapper.readTree(jsonCaptor.getValue());
        assertThat(json.get("conversationId").asString()).isEqualTo("conversation-\"redis\none");
        assertThat(json.get("lastOrderId").asString()).isEqualTo("order-1001");
    }
}
