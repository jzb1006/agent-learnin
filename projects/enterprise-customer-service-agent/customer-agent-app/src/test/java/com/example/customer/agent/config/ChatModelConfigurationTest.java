package com.example.customer.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.CustomerAgentApplication;
import com.example.customer.agent.chat.CustomerChatModelClient;
import com.example.customer.agent.chat.SpringAiCustomerChatModelClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

/**
 * 验证模型启用时客服模型客户端能完成装配。
 *
 * @author jiangzhibin
 * @since 2026-06-27 13:35:00
 */
class ChatModelConfigurationTest {

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
                Map.entry("customer-agent.chat-model.enabled", "true")));

        try (var context = application.run()) {
            assertThat(context.getBean(CustomerChatModelClient.class))
                    .isInstanceOf(SpringAiCustomerChatModelClient.class);
        }
    }
}
