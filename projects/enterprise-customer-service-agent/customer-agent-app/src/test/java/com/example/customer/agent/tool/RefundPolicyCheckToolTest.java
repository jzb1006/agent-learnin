package com.example.customer.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.order.MockOrderRepository;
import com.example.customer.domain.tool.ToolResultStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundPolicyCheckToolTest {

    private final RefundPolicyCheckTool tool = new RefundPolicyCheckTool(new MockOrderRepository());

    @Test
    void shouldExposeReadOnlyRefundPolicyCheckDefinition() {
        var definition = tool.definition();

        assertThat(definition.name()).isEqualTo("refund_policy_check");
        assertThat(definition.riskLevel().name()).isEqualTo("READ_ONLY");
        assertThat(definition.permission().executionAllowed()).isTrue();
        assertThat(definition.requiredParameterNames()).containsExactlyElementsOf(List.of("orderId", "tenantId"));
    }

    @Test
    void shouldReturnEligibleReviewWhenPaidOrderIsInsidePolicyWindow() {
        var result = tool.check("order-1001", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("orderId", "order-1001")
                .containsEntry("tenantId", "tenant-demo")
                .containsEntry("orderStatus", "PAID")
                .containsEntry("policyDecision", "ELIGIBLE_FOR_REVIEW")
                .containsEntry("recommendedAction", "CREATE_APPROVAL_REQUEST")
                .containsEntry("fundOperationExecuted", false);
        assertThat(result.payload().get("reason").toString()).contains("可进入人工审批");
        assertThat(result.errorCode()).isEmpty();
    }

    @Test
    void shouldRequireManualApprovalWhenPaidOrderIsOutsidePolicyWindow() {
        var result = tool.check("order-legacy-paid", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("orderId", "order-legacy-paid")
                .containsEntry("policyDecision", "REQUIRES_MANUAL_APPROVAL")
                .containsEntry("recommendedAction", "ESCALATE_TO_HUMAN_REVIEW")
                .containsEntry("fundOperationExecuted", false);
        assertThat(result.payload().get("reason").toString()).contains("超过");
    }

    @Test
    void shouldRejectAlreadyRefundedOrderWithoutFundOperation() {
        var result = tool.check("order-refunded", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("orderId", "order-refunded")
                .containsEntry("orderStatus", "REFUNDED")
                .containsEntry("policyDecision", "NOT_ELIGIBLE")
                .containsEntry("recommendedAction", "EXPLAIN_POLICY")
                .containsEntry("fundOperationExecuted", false);
        assertThat(result.payload().get("reason").toString()).contains("已退款");
    }

    @Test
    void shouldNotExposeRefundPolicyAcrossTenants() {
        var result = tool.check("order-1001", "tenant-other");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("ORDER_NOT_FOUND");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("tenant"));
    }

    @Test
    void shouldReturnInvalidArgumentWhenOrderIdIsBlank() {
        var result = tool.check(" ", "tenant-demo");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("INVALID_ARGUMENT");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("orderId"));
    }
}
