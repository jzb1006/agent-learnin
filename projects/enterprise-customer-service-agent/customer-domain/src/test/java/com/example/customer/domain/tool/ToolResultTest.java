package com.example.customer.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolResultTest {

    @Test
    void shouldRepresentSuccessfulToolResult() {
        var result = ToolResult.succeeded(
                "order_lookup",
                Map.of("orderStatus", "PAID", "courseName", "企业级 AI Agent 实战营"));

        assertThat(result.toolName()).isEqualTo("order_lookup");
        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.payload()).containsEntry("orderStatus", "PAID");
        assertThat(result.errorCode()).isEmpty();
        assertThat(result.errorMessage()).isEmpty();
    }

    @Test
    void shouldRepresentFailedToolResultWithExplicitCode() {
        var result = ToolResult.failed(
                "order_lookup",
                "ORDER_NOT_FOUND",
                "订单不存在或不属于当前租户");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).contains("ORDER_NOT_FOUND");
        assertThat(result.errorMessage()).contains("订单不存在或不属于当前租户");
    }

    @Test
    void shouldRejectFailedResultWithoutErrorCode() {
        assertThatThrownBy(() -> ToolResult.failed("order_lookup", " ", "订单不存在"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("error code must not be blank");
    }
}
