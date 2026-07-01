package com.example.customer.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.customer.agent.config.CustomerAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

class ChatMemoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MemoryTestConfiguration.class))
            .withUserConfiguration(
                    ConversationSummaryCompressor.class,
                    InMemoryChatMemory.class,
                    RedisChatMemory.class);

    @Test
    void shouldUseInMemoryMemoryWhenConfiguredForLocalTests() {
        contextRunner
                .withPropertyValues("customer-agent.conversation-memory.storage=in-memory")
                .run(context -> assertThat(context.getBean(ChatMemory.class)).isInstanceOf(InMemoryChatMemory.class));
    }

    @Test
    void shouldUseRedisMemoryWhenConfiguredForProduction() {
        contextRunner
                .withPropertyValues("customer-agent.conversation-memory.storage=redis")
                .run(context -> assertThat(context.getBean(ChatMemory.class)).isInstanceOf(RedisChatMemory.class));
    }

    @Configuration
    @EnableConfigurationProperties(CustomerAgentProperties.class)
    static class MemoryTestConfiguration {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }
    }
}
