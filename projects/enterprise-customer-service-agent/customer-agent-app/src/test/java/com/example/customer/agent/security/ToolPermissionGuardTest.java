package com.example.customer.agent.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.domain.approval.ApprovalAction;
import com.example.customer.domain.tool.ToolRiskLevel;
import org.junit.jupiter.api.Test;

class ToolPermissionGuardTest {

    @Test
    void shouldAllowReadOnlyToolExecution() {
        var guard = new ToolPermissionGuard();

        var decision = guard.requireAllowed("order_lookup", ToolRiskLevel.READ_ONLY, ApprovalAction.ORDER_LOOKUP);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.approvalRequired()).isFalse();
    }

    @Test
    void shouldBlockHighRiskToolBeforeExecution() {
        var guard = new ToolPermissionGuard();

        assertThatThrownBy(() -> guard.requireAllowed("refund_execute", ToolRiskLevel.HIGH_RISK, ApprovalAction.REFUND_ORDER))
                .isInstanceOf(ToolPermissionDeniedException.class)
                .hasMessageContaining("refund_execute")
                .hasMessageContaining("approval");
    }
}
