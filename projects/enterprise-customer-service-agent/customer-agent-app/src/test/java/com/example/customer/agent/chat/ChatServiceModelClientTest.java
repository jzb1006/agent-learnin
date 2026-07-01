package com.example.customer.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.intent.IntentRouter;
import com.example.customer.agent.mcp.FakeMcpToolClient;
import com.example.customer.agent.mcp.McpToolCallRequest;
import com.example.customer.agent.mcp.McpToolCallResponse;
import com.example.customer.agent.mcp.McpToolClient;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.trace.ConversationRoute;
import com.example.customer.domain.tool.ToolResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceModelClientTest {

    @Test
    void shouldDependOnMcpToolClientInsteadOfConcreteToolImplementations() {
        var constructorParameterTypes = Arrays.stream(ChatService.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .toList();

        assertThat(constructorParameterTypes)
                .contains(McpToolClient.class)
                .extracting(Class::getPackageName)
                .doesNotContain("com.example.customer.agent.tool");
    }

    @Test
    void shouldUseConfiguredChatModelClientWhenEnabled() {
        var properties = properties(true);
        var chatModelClient = new RecordingChatModelClient(List.of("""
                {
                  "route": "ORDER_LOOKUP",
                  "answer": "模型回复：订单已支付，下周一开课。",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单状态", "等待用户继续追问"],
                  "traceId": "trace-model-ignored"
                }
                """));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课"));

        assertThat(response.answer()).isEqualTo("模型回复：订单已支付，下周一开课。");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.traceId()).startsWith("trace-");
        assertThat(chatModelClient.prompts()).hasSize(1);
        assertThat(chatModelClient.prompts().getFirst().tenantId()).isEqualTo("tenant-demo");
        assertThat(chatModelClient.prompts().getFirst().message()).contains("order-1001");
        assertThat(chatModelClient.prompts().getFirst().orderEvidence()).contains("企业级 AI Agent 实战营");
        assertThat(chatModelClient.prompts().getFirst().orderEvidence())
                .contains("route=ORDER_LOOKUP")
                .contains("riskLevel=READ_ONLY")
                .contains("sources=[order:order-1001]")
                .contains("traceId=");
    }

    @Test
    void shouldAnswerOrderNumberQuestionWithServerRouteMetadata() {
        var properties = properties(true);
        var chatModelClient = new RecordingChatModelClient(List.of("""
                {
                  "route": "ORDER_LOOKUP",
                  "answer": "你的演示订单号是 order-1001。",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单号", "等待用户继续追问"],
                  "traceId": "trace-model-order-number"
                }
                """));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "我的订单号是多少"));

        assertThat(response.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP.name());
        assertThat(response.riskLevel()).isEqualTo("READ_ONLY");
        assertThat(response.answer()).contains("order-1001");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(chatModelClient.prompts().getFirst().orderEvidence())
                .contains("route=ORDER_LOOKUP")
                .contains("sources=[order:order-1001]");
    }

    @Test
    void shouldFallbackToDeterministicReplyWhenChatModelDisabled() {
        var properties = properties(false);
        var chatModelClient = new RecordingChatModelClient(List.of("不应该使用模型"));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课"));

        assertThat(response.answer()).contains("已查询到订单 order-1001");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo("order_lookup");
                    assertThat(toolCall.arguments()).containsEntry("orderId", "order-1001");
                    assertThat(toolCall.arguments()).containsEntry("tenantId", "tenant-demo");
                    assertThat(toolCall.status()).isEqualTo("SUCCEEDED");
                    assertThat(toolCall.riskLevel()).isEqualTo("READ_ONLY");
                    assertThat(toolCall.durationMs()).isGreaterThanOrEqualTo(0L);
                    assertThat(toolCall.resultSummary()).contains("PAID");
                });
        assertThat(chatModelClient.prompts()).isEmpty();
    }

    @Test
    void shouldReturnStructuredResponseWhenMcpToolCallFails() {
        var properties = properties(false);
        var service = chatService(properties, new RecordingChatModelClient(List.of()), new FailingMcpToolClient());

        var response = service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课"));

        assertThat(response.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP.name());
        assertThat(response.riskLevel()).isEqualTo("READ_ONLY");
        assertThat(response.answer()).contains("未查询到可用于回复的订单证据").contains("MCP_CLIENT_CALL_FAILED");
        assertThat(response.sources()).isEmpty();
        assertThat(response.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo("order_lookup");
                    assertThat(toolCall.status()).isEqualTo("FAILED");
                    assertThat(toolCall.resultSummary()).contains("MCP_CLIENT_CALL_FAILED");
                });
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
    void shouldFallbackToDeterministicReplyWhenModelReturnsWrongRouteAndBlankTraceId() {
        var properties = properties(true);
        var chatModelClient = new RecordingChatModelClient(List.of("""
                {
                  "route": "HUMAN_HANDOFF",
                  "answer": "您好，为了保障您的账户安全，需要先验证您的身份才能查询订单号。请提供您的手机号或注册邮箱。",
                  "sources": [],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["请提供您的手机号或邮箱以验证身份"],
                  "traceId": ""
                }
                """));
        var service = chatService(properties, chatModelClient);

        var response = service.reply(new ChatRequest("tenant-demo", "我的订单号是多少"));

        assertThat(response.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP.name());
        assertThat(response.riskLevel()).isEqualTo("READ_ONLY");
        assertThat(response.answer()).contains("已查询到订单 order-1001");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.nextActions()).containsExactly("展示订单状态", "等待用户继续追问");
    }

    @Test
    void shouldExposeRefundOrCancelIntentWithoutExecutingRiskyAction() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "订单 order-1001 可以退款吗？"));

        assertThat(response.route()).isEqualTo(ConversationRoute.REFUND_OR_CANCEL.name());
        assertThat(response.riskLevel()).isEqualTo("HIGH_RISK");
        assertThat(response.answer()).contains("可进入人工审批流程");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.nextActions()).contains("创建人工审批请求");
        assertThat(response.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo("refund_policy_check");
                    assertThat(toolCall.arguments()).containsEntry("orderId", "order-1001");
                    assertThat(toolCall.arguments()).containsEntry("tenantId", "tenant-demo");
                    assertThat(toolCall.status()).isEqualTo("SUCCEEDED");
                    assertThat(toolCall.riskLevel()).isEqualTo("READ_ONLY");
                    assertThat(toolCall.resultSummary()).contains("ELIGIBLE_FOR_REVIEW");
                });
    }

    @Test
    void shouldExposeKnowledgeQaIntentWithoutOrderEvidence() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "新手适合学企业级 AI Agent 课程吗？"));

        assertThat(response.route()).isEqualTo(ConversationRoute.KNOWLEDGE_QA.name());
        assertThat(response.riskLevel()).isEqualTo("READ_ONLY");
        assertThat(response.answer()).contains("不建议完全零基础学员");
        assertThat(response.sources()).contains("week10/work_v3/datas/data.txt#Q1-Q6");
        assertThat(response.nextActions()).contains("展示知识库来源");
        assertThat(response.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo("kb_search");
                    assertThat(toolCall.arguments()).containsEntry("tenantId", "default");
                    assertThat(toolCall.arguments()).containsEntry("query", "新手适合学企业级 AI Agent 课程吗？");
                    assertThat(toolCall.status()).isEqualTo("SUCCEEDED");
                    assertThat(toolCall.riskLevel()).isEqualTo("READ_ONLY");
                    assertThat(toolCall.resultSummary()).contains("课程适合哪些学员");
                });
    }

    @Test
    void shouldExposeHumanHandoffIntentWithoutCreatingTicket() {
        var response = chatService(properties(false), new RecordingChatModelClient(List.of()))
                .reply(new ChatRequest("tenant-demo", "我要转人工客服"));

        assertThat(response.route()).isEqualTo(ConversationRoute.HUMAN_HANDOFF.name());
        assertThat(response.riskLevel()).isEqualTo("LOW_RISK_WRITE");
        assertThat(response.answer()).contains("人工客服");
        assertThat(response.sources()).isEmpty();
        assertThat(response.nextActions()).contains("记录人工转接意向");
    }

    @Test
    void shouldResolveContextualOrderReferenceFromSameConversationMemory() {
        var service = chatService(properties(false), new RecordingChatModelClient(List.of()));

        var firstResponse = service.reply(new ChatRequest(
                "tenant-demo",
                "帮我查询订单 order-1001 什么时候开课",
                "conversation-day24"));
        var secondResponse = service.reply(new ChatRequest(
                "tenant-demo",
                "刚才那个订单可以退款吗？",
                "conversation-day24"));

        assertThat(firstResponse.conversationId()).isEqualTo("conversation-day24");
        assertThat(firstResponse.memorySummary()).contains("order-1001");
        assertThat(secondResponse.route()).isEqualTo(ConversationRoute.REFUND_OR_CANCEL.name());
        assertThat(secondResponse.sources()).containsExactly("order:order-1001");
        assertThat(secondResponse.answer()).contains("退款政策前置检查");
        assertThat(secondResponse.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo("refund_policy_check");
                    assertThat(toolCall.arguments()).containsEntry("orderId", "order-1001");
                    assertThat(toolCall.arguments()).containsEntry("tenantId", "tenant-demo");
                    assertThat(toolCall.status()).isEqualTo("SUCCEEDED");
                });
        assertThat(secondResponse.memorySummary()).contains("order-1001");
    }

    @Test
    void shouldKeepConversationMemoryTenantScoped() {
        var service = chatService(properties(false), new RecordingChatModelClient(List.of()));

        service.reply(new ChatRequest("tenant-demo", "帮我查询订单 order-1001 什么时候开课", "conversation-shared"));
        var response = service.reply(new ChatRequest("tenant-other", "刚才那个订单可以退款吗？", "conversation-shared"));

        assertThat(response.route()).isEqualTo(ConversationRoute.REFUND_OR_CANCEL.name());
        assertThat(response.sources()).isEmpty();
        assertThat(response.answer()).contains("未找到可用于政策判断的订单证据");
        assertThat(response.toolCalls()).isEmpty();
    }

    @Test
    void shouldCompressConversationSummaryBeforeSendingPromptToModel() {
        var properties = properties(true);
        properties.getConversationMemory().setMaxSummaryChars(120);
        properties.getConversationMemory().setMaxMessageChars(24);
        var longMessage = "帮我查询订单 order-1001 什么时候开课，"
                + "这里附带一段很长很长的上下文，".repeat(12)
                + "请不要把原文完整塞进下一轮模型提示。";
        var chatModelClient = new RecordingChatModelClient(List.of(
                """
                {
                  "route": "ORDER_LOOKUP",
                  "answer": "已查询到订单 order-1001。",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单状态"],
                  "traceId": "trace-model-memory-1"
                }
                """,
                """
                {
                  "route": "ORDER_LOOKUP",
                  "answer": "刚才那个订单是企业级 AI Agent 实战营。",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单状态"],
                  "traceId": "trace-model-memory-2"
                }
                """));
        var service = chatService(properties, chatModelClient);

        service.reply(new ChatRequest("tenant-demo", longMessage, "conversation-compress"));
        service.reply(new ChatRequest("tenant-demo", "刚才那个订单是什么课程？", "conversation-compress"));

        assertThat(chatModelClient.prompts()).hasSize(2);
        var secondPrompt = chatModelClient.prompts().get(1);
        assertThat(secondPrompt.memorySummary()).contains("order-1001");
        assertThat(secondPrompt.memorySummary()).hasSizeLessThanOrEqualTo(120);
        assertThat(secondPrompt.memorySummary()).doesNotContain("请不要把原文完整塞进下一轮模型提示");
    }

    private static CustomerAgentProperties properties(boolean enabled) {
        var properties = new CustomerAgentProperties();
        properties.getChatModel().setEnabled(enabled);
        return properties;
    }

    private static ChatService chatService(CustomerAgentProperties properties, CustomerChatModelClient chatModelClient) {
        return chatService(properties, chatModelClient, new FakeMcpToolClient());
    }

    private static ChatService chatService(
            CustomerAgentProperties properties,
            CustomerChatModelClient chatModelClient,
            McpToolClient mcpToolClient) {
        return new ChatService(
                properties,
                chatModelClient,
                new IntentRouter(),
                new CustomerAgentResponseParser(new tools.jackson.databind.ObjectMapper()),
                mcpToolClient,
                new com.example.customer.agent.memory.InMemoryChatMemory(
                        properties,
                        new com.example.customer.agent.memory.ConversationSummaryCompressor(properties)));
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

    private static final class FailingMcpToolClient implements McpToolClient {

        @Override
        public List<ToolDefinition> listTools() {
            return List.of();
        }

        @Override
        public McpToolCallResponse call(McpToolCallRequest request) {
            return new McpToolCallResponse(
                    request.toolName(),
                    ToolResult.failed(request.toolName(), "MCP_CLIENT_CALL_FAILED", "MCP 工具调用失败: timeout"),
                    1000);
        }
    }
}
