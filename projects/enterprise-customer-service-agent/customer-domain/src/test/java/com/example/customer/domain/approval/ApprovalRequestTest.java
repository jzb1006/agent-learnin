package com.example.customer.domain.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApprovalRequestTest {

    @Test
    void shouldCreatePendingApprovalForHighRiskAction() {
        var request = ApprovalRequest.pending(
                "approval-1",
                "tenant-education",
                "order-1001",
                ApprovalAction.REFUND_ORDER,
                ToolRiskLevel.HIGH_RISK,
                "User asked for a refund.",
                Instant.parse("2026-06-27T00:00:00Z"));

        assertThat(request.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(request.requiresHumanDecision()).isTrue();
    }

    @Test
    void shouldRejectApprovalForReadOnlyTool() {
        assertThatThrownBy(() -> ApprovalRequest.pending(
                        "approval-1",
                        "tenant-education",
                        "order-1001",
                        ApprovalAction.ORDER_LOOKUP,
                        ToolRiskLevel.READ_ONLY,
                        "Order lookup is read only.",
                        Instant.parse("2026-06-27T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approval");
    }
}
