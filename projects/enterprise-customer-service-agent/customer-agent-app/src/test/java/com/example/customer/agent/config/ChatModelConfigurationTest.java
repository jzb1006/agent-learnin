package com.example.customer.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.CustomerAgentApplication;
import com.example.customer.agent.chat.CustomerChatModelClient;
import com.example.customer.agent.chat.SpringAiCustomerChatModelClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.WebApplicationType;
import org.springframework.mock.env.MockEnvironment;

/**
 * 验证模型启用时客服模型客户端能完成装配。
 *
 * @author jiangzhibin
 * @since 2026-06-27 13:35:00
 */
class ChatModelConfigurationTest {

    @Test
    void shouldBindSeparateChatAndEmbeddingOpenAiConnections() {
        new ApplicationContextRunner()
                .withUserConfiguration(OpenAiConnectionPropertiesConfiguration.class)
                .withPropertyValues(
                        "spring.ai.openai.api-key=legacy-key",
                        "spring.ai.openai.base-url=https://api.deepseek.com",
                        "spring.ai.openai.chat.api-key=chat-key",
                        "spring.ai.openai.chat.base-url=https://api.deepseek.com",
                        "spring.ai.openai.chat.model=deepseek-v4-flash",
                        "spring.ai.openai.embedding.api-key=embedding-key",
                        "spring.ai.openai.embedding.base-url=https://embedding.example.com",
                        "spring.ai.openai.embedding.model=text-embedding-3-small",
                        "spring.ai.openai.embedding.dimensions=1536")
                .run(context -> {
                    var chat = context.getBean(OpenAiChatProperties.class);
                    var embedding = context.getBean(OpenAiEmbeddingProperties.class);

                    assertThat(chat.getApiKey()).isEqualTo("chat-key");
                    assertThat(chat.getBaseUrl()).isEqualTo("https://api.deepseek.com");
                    assertThat(chat.getModel()).isEqualTo("deepseek-v4-flash");
                    assertThat(embedding.getApiKey()).isEqualTo("embedding-key");
                    assertThat(embedding.getBaseUrl()).isEqualTo("https://embedding.example.com");
                    assertThat(embedding.getModel()).isEqualTo("text-embedding-3-small");
                    assertThat(embedding.getDimensions()).isEqualTo(1536);
                });
    }

    @Test
    void shouldResolveApplicationYamlWithSeparateChatAndEmbeddingEnvVars() {
        var environment = new MockEnvironment();
        TestPropertyValues.of(Map.ofEntries(
                        Map.entry("spring.ai.openai.chat.api-key", "${CUSTOMER_AGENT_CHAT_OPENAI_API_KEY:${SPRING_AI_OPENAI_API_KEY:}}"),
                        Map.entry("spring.ai.openai.chat.base-url", "${CUSTOMER_AGENT_CHAT_OPENAI_BASE_URL:${SPRING_AI_OPENAI_BASE_URL:https://api.deepseek.com}}"),
                        Map.entry("spring.ai.openai.chat.model", "${CUSTOMER_AGENT_CHAT_OPENAI_MODEL:${SPRING_AI_OPENAI_CHAT_MODEL:deepseek-v4-flash}}"),
                        Map.entry("spring.ai.openai.embedding.api-key", "${CUSTOMER_AGENT_EMBEDDING_OPENAI_API_KEY:${SPRING_AI_OPENAI_EMBEDDING_API_KEY:${SPRING_AI_OPENAI_API_KEY:}}}"),
                        Map.entry("spring.ai.openai.embedding.base-url", "${CUSTOMER_AGENT_EMBEDDING_OPENAI_BASE_URL:${SPRING_AI_OPENAI_EMBEDDING_BASE_URL:${SPRING_AI_OPENAI_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}}}"),
                        Map.entry("spring.ai.openai.embedding.model", "${CUSTOMER_AGENT_EMBEDDING_OPENAI_MODEL:${SPRING_AI_OPENAI_EMBEDDING_MODEL:text-embedding-v4}}"),
                        Map.entry("spring.ai.openai.embedding.dimensions", "${CUSTOMER_AGENT_EMBEDDING_DIMENSIONS:${CUSTOMER_AGENT_PGVECTOR_DIMENSIONS:1536}}"),
                        Map.entry("CUSTOMER_AGENT_CHAT_OPENAI_API_KEY", "deepseek-key"),
                        Map.entry("CUSTOMER_AGENT_CHAT_OPENAI_BASE_URL", "https://api.deepseek.com"),
                        Map.entry("CUSTOMER_AGENT_CHAT_OPENAI_MODEL", "deepseek-v4-flash"),
                        Map.entry("CUSTOMER_AGENT_EMBEDDING_OPENAI_API_KEY", "dashscope-key"),
                        Map.entry("CUSTOMER_AGENT_EMBEDDING_OPENAI_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
                        Map.entry("CUSTOMER_AGENT_EMBEDDING_OPENAI_MODEL", "text-embedding-v4"),
                        Map.entry("CUSTOMER_AGENT_EMBEDDING_DIMENSIONS", "1536")))
                .applyTo(environment);
        var binder = Binder.get(environment);
        var chat = binder.bind("spring.ai.openai.chat", OpenAiChatProperties.class)
                .orElseThrow(() -> new IllegalStateException("chat properties are not bound"));
        var embedding = binder.bind("spring.ai.openai.embedding", OpenAiEmbeddingProperties.class)
                .orElseThrow(() -> new IllegalStateException("embedding properties are not bound"));

        assertThat(chat.getApiKey()).isEqualTo("deepseek-key");
        assertThat(chat.getBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(chat.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(embedding.getApiKey()).isEqualTo("dashscope-key");
        assertThat(embedding.getBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
        assertThat(embedding.getModel()).isEqualTo("text-embedding-v4");
        assertThat(embedding.getDimensions()).isEqualTo(1536);
    }

    @Test
    void shouldCreateCustomerChatModelClientWhenSpringAiModelIsEnabled() {
        var application = new SpringApplication(CustomerAgentApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.ofEntries(
                Map.entry("spring.config.name", "chat-model-configuration-test"),
                Map.entry("spring.autoconfigure.exclude", String.join(",",
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
                        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration")),
                Map.entry("spring.ai.model.audio.speech", "none"),
                Map.entry("spring.ai.model.audio.transcription", "none"),
                Map.entry("spring.ai.model.chat", "openai"),
                Map.entry("spring.ai.model.embedding", "none"),
                Map.entry("spring.ai.model.image", "none"),
                Map.entry("spring.ai.model.moderation", "none"),
                Map.entry("spring.ai.openai.api-key", "test-key"),
                Map.entry("spring.ai.openai.base-url", "https://api.deepseek.com"),
                Map.entry("spring.ai.openai.chat.model", "deepseek-v4-flash"),
                Map.entry("customer-agent.chat-model.enabled", "true"),
                Map.entry("customer-agent.conversation-memory.storage", "in-memory")));

        try (var context = application.run()) {
            assertThat(context.getBean(CustomerChatModelClient.class))
                    .isInstanceOf(SpringAiCustomerChatModelClient.class);
        }
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties({OpenAiChatProperties.class, OpenAiEmbeddingProperties.class})
    static class OpenAiConnectionPropertiesConfiguration {
    }
}
