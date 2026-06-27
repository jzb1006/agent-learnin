package com.example.customer.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.intent.IntentRouter;
import com.example.customer.agent.order.MockOrderRepository;
import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceModelClientTest {

    @Test
    void shouldUseConfiguredChatModelClientWhenEnabled() {
        var properties = properties(true);
        var chatModelClient = new RecordingChatModelClient(List.of("模型回复：订单已支付，下周一开课。"));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课"));

        assertThat(response.reply()).isEqualTo("模型回复：订单已支付，下周一开课。");
        assertThat(response.order().id()).isEqualTo("order-1001");
        assertThat(chatModelClient.prompts()).hasSize(1);
        assertThat(chatModelClient.prompts().getFirst().tenantId()).isEqualTo("tenant-demo");
        assertThat(chatModelClient.prompts().getFirst().message()).contains("order-1001");
        assertThat(chatModelClient.prompts().getFirst().orderEvidence()).contains("企业级 AI Agent 实战营");
    }

    @Test
    void shouldFallbackToDeterministicReplyWhenChatModelDisabled() {
        var properties = properties(false);
        var chatModelClient = new RecordingChatModelClient(List.of("不应该使用模型"));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课"));

        assertThat(response.reply()).contains("已查询到订单 order-1001");
        assertThat(chatModelClient.prompts()).isEmpty();
    }

    @Test
    void shouldWrapModelFailureAsBusinessException() {
        var properties = properties(true);
        var service = chatService(properties, new FailingChatModelClient());

        assertThatThrownBy(() -> service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001")))
                .isInstanceOf(ChatModelException.class)
                .hasMessageContaining("模型调用失败");
    }

    @Test
    void shouldExposeRefundOrCancelIntentWithoutExecutingRiskyAction() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "订单 order-1001 可以退款吗？"));

        assertThat(response.route()).isEqualTo(ConversationRoute.REFUND_OR_CANCEL.name());
        assertThat(response.riskLevel()).isEqualTo("HIGH_RISK");
        assertThat(response.reply()).contains("不能直接执行退款");
        assertThat(response.order().id()).isEqualTo("order-1001");
        assertThat(response.nextActions()).contains("进入人工审批前置判断");
    }

    @Test
    void shouldExposeKnowledgeQaIntentWithoutOrderEvidence() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "新手适合学企业级 AI Agent 课程吗？"));

        assertThat(response.route()).isEqualTo(ConversationRoute.KNOWLEDGE_QA.name());
        assertThat(response.riskLevel()).isEqualTo("READ_ONLY");
        assertThat(response.reply()).contains("知识库");
        assertThat(response.order()).isNull();
        assertThat(response.nextActions()).contains("等待 RAG 知识库接入");
    }

    @Test
    void shouldExposeHumanHandoffIntentWithoutCreatingTicket() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "我要转人工客服"));

        assertThat(response.route()).isEqualTo(ConversationRoute.HUMAN_HANDOFF.name());
        assertThat(response.riskLevel()).isEqualTo("LOW_RISK_WRITE");
        assertThat(response.reply()).contains("人工客服");
        assertThat(response.order()).isNull();
        assertThat(response.nextActions()).contains("记录人工转接意向");
    }

    private static CustomerAgentProperties properties(boolean enabled) {
        var properties = new CustomerAgentProperties();
        properties.getChatModel().setEnabled(enabled);
        return properties;
    }

    private static OrderLookupService orderLookupService() {
        return new OrderLookupService(new MockOrderRepository());
    }

    private static ChatService chatService(CustomerAgentProperties properties, CustomerChatModelClient chatModelClient) {
        return new ChatService(orderLookupService(), properties, chatModelClient, new IntentRouter());
    }

    private static final class RecordingChatModelClient implements CustomerChatModelClient {

        private final List<String> replies;
        private final List<CustomerChatPrompt> prompts = new ArrayList<>();

        private RecordingChatModelClient(List<String> replies) {
            this.replies = replies;
        }

        @Override
        public String generateReply(CustomerChatPrompt prompt) {
            prompts.add(prompt);
            return replies.getFirst();
        }

        private List<CustomerChatPrompt> prompts() {
            return prompts;
        }
    }

    private static final class FailingChatModelClient implements CustomerChatModelClient {

        @Override
        public String generateReply(CustomerChatPrompt prompt) {
            throw new ChatModelException("模型调用失败", new RuntimeException("timeout"));
        }
    }
}
