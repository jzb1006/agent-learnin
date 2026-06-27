package com.example.customer.domain.trace;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConversationTraceTest {

    @Test
    void shouldAppendToolCallWithoutMutatingOriginalTrace() {
        var trace = ConversationTrace.started(
                "trace-1",
                "tenant-education",
                "Where is my order?",
                ConversationRoute.ORDER_LOOKUP,
                Instant.parse("2026-06-27T00:00:00Z"));
        var toolCall = ToolCallRecord.succeeded(
                "order_lookup",
                ToolRiskLevel.READ_ONLY,
                Duration.ofMillis(35));

        var updated = trace.appendToolCall(toolCall);

        assertThat(trace.toolCalls()).isEmpty();
        assertThat(updated.toolCalls()).containsExactly(toolCall);
        assertThat(updated.route()).isEqualTo(ConversationRoute.ORDER_LOOKUP);
    }
}
