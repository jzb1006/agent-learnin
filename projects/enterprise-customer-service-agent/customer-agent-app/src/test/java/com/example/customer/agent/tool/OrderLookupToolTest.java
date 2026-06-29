package com.example.customer.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.order.MockOrderRepository;
import com.example.customer.domain.tool.ToolResultStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderLookupToolTest {

    private final OrderLookupTool tool = new OrderLookupTool(new MockOrderRepository());

    @Test
    void shouldExposeReadOnlyOrderLookupDefinition() {
        var definition = tool.definition();

        assertThat(definition.name()).isEqualTo("order_lookup");
        assertThat(definition.riskLevel().name()).isEqualTo("READ_ONLY");
        assertThat(definition.permission().executionAllowed()).isTrue();
        assertThat(definition.requiredParameterNames()).containsExactlyElementsOf(List.of("orderId", "tenantId"));
    }

    @Test
    void shouldReturnOrderPayloadForSameTenant() {
        var result = tool.lookup("order-1001", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("orderId", "order-1001")
                .containsEntry("tenantId", "tenant-demo")
                .containsEntry("customerId", "customer-1001")
                .containsEntry("productName", "企业级 AI Agent 实战营")
                .containsEntry("status", "PAID");
        assertThat(result.errorCode()).isEmpty();
    }

    @Test
    void shouldReturnExplicitFailureWhenOrderDoesNotExist() {
        var result = tool.lookup("missing-order", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("ORDER_NOT_FOUND");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("missing-order"));
    }

    @Test
    void shouldNotExposeOrderAcrossTenants() {
        var result = tool.lookup("order-1001", "tenant-other");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("ORDER_NOT_FOUND");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("tenant"));
    }

    @Test
    void shouldReturnInvalidArgumentWhenRequiredInputIsBlank() {
        var result = tool.lookup(" ", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("INVALID_ARGUMENT");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("orderId"));
    }
}
