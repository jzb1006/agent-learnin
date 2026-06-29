package com.example.customer.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CustomerAgentResponseParserTest {

    private final CustomerAgentResponseParser parser = new CustomerAgentResponseParser(new ObjectMapper());

    @Test
    void shouldParseValidModelResponseToCustomerAgentResponse() {
        var rawResponse = """
                {
                  "route": "ORDER_LOOKUP",
                  "answer": "订单 order-1001 已支付，下周一开课。",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单状态", "等待用户继续追问"],
                  "traceId": "trace-structured-test"
                }
                """;

        var response = parser.parse(rawResponse);

        assertThat(response.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP.name());
        assertThat(response.answer()).contains("order-1001");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.riskLevel()).isEqualTo(ToolRiskLevel.READ_ONLY.name());
        assertThat(response.nextActions()).containsExactly("展示订单状态", "等待用户继续追问");
        assertThat(response.traceId()).isEqualTo("trace-structured-test");
    }

    @Test
    void shouldRejectResponseWithMissingAnswerField() {
        var rawResponse = """
                {
                  "route": "ORDER_LOOKUP",
                  "sources": ["order:order-1001"],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["展示订单状态"],
                  "traceId": "trace-missing-answer"
                }
                """;

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(CustomerAgentResponseParseException.class)
                .hasMessageContaining("answer");
    }

    @Test
    void shouldRejectResponseWithUnknownRoute() {
        var rawResponse = """
                {
                  "route": "UNKNOWN",
                  "answer": "无法识别路由。",
                  "sources": [],
                  "riskLevel": "READ_ONLY",
                  "nextActions": ["转人工确认"],
                  "traceId": "trace-invalid-route"
                }
                """;

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(CustomerAgentResponseParseException.class)
                .hasMessageContaining("route")
                .hasMessageContaining("UNKNOWN");
    }
}
