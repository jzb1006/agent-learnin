package com.example.customer.agent.config;

import com.example.customer.agent.chat.CustomerChatModelClient;
import com.example.customer.agent.chat.DisabledCustomerChatModelClient;
import com.example.customer.agent.chat.SpringAiCustomerChatModelClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 客服模型客户端装配配置。
 * <p>
 * 显式区分默认禁用模式和真实模型模式，避免本地测试误调用外部模型。
 *
 * @author jiangzhibin
 * @since 2026-06-27 11:00:00
 */
@Configuration
public class ChatModelConfiguration {

    /**
     * 启用真实模型时装配 Spring AI ChatClient 适配器。
     *
     * @param chatClientBuilder Spring AI 自动配置的 ChatClient Builder
     * @return 客服模型客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "customer-agent.chat-model", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(CustomerChatModelClient.class)
    public CustomerChatModelClient springAiCustomerChatModelClient(ChatClient.Builder chatClientBuilder) {
        return new SpringAiCustomerChatModelClient(chatClientBuilder);
    }

    /**
     * 默认未启用模型时装配禁用客户端。
     *
     * @return 客服模型客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "customer-agent.chat-model", name = "enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(CustomerChatModelClient.class)
    public CustomerChatModelClient disabledCustomerChatModelClient() {
        return new DisabledCustomerChatModelClient();
    }
}
