package com.example.customer.agent.chat;

import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 基于 Spring AI ChatClient 的客服模型客户端。
 *
 * @author jiangzhibin
 * @since 2026-06-27 10:55:00
 */
@Slf4j
public class SpringAiCustomerChatModelClient implements CustomerChatModelClient {

    private static final String SYSTEM_PROMPT = """
            你是企业级智能客服与订单协同 Agent。
            你只能基于输入的订单证据回答，不得编造订单状态、课程时间或退款承诺。
            回复必须简洁、专业，并在信息不足时提示转人工或继续确认。
            """;

    private final ChatClient chatClient;

    /**
     * 创建 Spring AI 客服模型客户端。
     *
     * @param chatClientBuilder Spring AI 自动配置的 ChatClient Builder
     */
    public SpringAiCustomerChatModelClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 调用 ChatClient 生成客服回复。
     *
     * @param prompt 客服对话提示词上下文
     * @return 模型回复
     */
    @Override
    public String generateReply(CustomerChatPrompt prompt) {
        var startedAt = Instant.now();
        try {
            log.info("spring_ai_chat_call_start tenantId={} messageLength={}", prompt.tenantId(), prompt.message().length());
            var content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("""
                            租户：%s
                            用户问题：%s
                            订单证据：%s
                            """.formatted(prompt.tenantId(), prompt.message(), prompt.orderEvidence()))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new ChatModelException("模型调用失败：返回内容为空", null);
            }
            log.info("spring_ai_chat_call_success tenantId={} durationMs={}",
                    prompt.tenantId(),
                    Duration.between(startedAt, Instant.now()).toMillis());
            return content;
        } catch (ChatModelException exception) {
            log.warn("spring_ai_chat_call_empty_or_invalid tenantId={} durationMs={}",
                    prompt.tenantId(),
                    Duration.between(startedAt, Instant.now()).toMillis());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("spring_ai_chat_call_failed tenantId={} durationMs={}",
                    prompt.tenantId(),
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    exception);
            throw new ChatModelException("模型调用失败", exception);
        }
    }
}
