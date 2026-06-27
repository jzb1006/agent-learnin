package com.example.customer.agent.intent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.trace.ConversationRoute;
import org.junit.jupiter.api.Test;

class IntentRouterTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    void shouldRouteKnowledgeQuestionToKnowledgeQa() {
        var result = router.route("新手适合学企业级 AI Agent 课程吗？");

        assertThat(result.route()).isEqualTo(ConversationRoute.KNOWLEDGE_QA);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(result.reason()).contains("知识");
    }

    @Test
    void shouldRouteOrderQuestionToOrderLookupAndExtractOrderId() {
        var result = router.route("帮我查询订单 order-1001 什么时候开课");

        assertThat(result.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP);
        assertThat(result.orderId()).isEqualTo("order-1001");
        assertThat(result.reason()).contains("订单");
    }

    @Test
    void shouldPreferRefundOrCancelOverOrderLookupWhenMessageContainsBoth() {
        var result = router.route("订单 order-1001 可以退款吗？");

        assertThat(result.route()).isEqualTo(ConversationRoute.REFUND_OR_CANCEL);
        assertThat(result.orderId()).isEqualTo("order-1001");
        assertThat(result.reason()).contains("退款");
    }

    @Test
    void shouldRouteHumanHandoffRequestToHumanHandoff() {
        var result = router.route("我不想继续和机器人说了，转人工客服");

        assertThat(result.route()).isEqualTo(ConversationRoute.HUMAN_HANDOFF);
        assertThat(result.orderId()).isNull();
        assertThat(result.reason()).contains("人工");
    }

    @Test
    void shouldFallbackUnknownMessageToDirect() {
        var result = router.route("你好");

        assertThat(result.route()).isEqualTo(ConversationRoute.DIRECT);
        assertThat(result.confidence()).isLessThan(0.70);
        assertThat(result.reason()).contains("fallback");
    }
}
