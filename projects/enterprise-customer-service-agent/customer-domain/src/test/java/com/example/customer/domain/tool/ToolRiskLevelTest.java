package com.example.customer.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ToolRiskLevelTest {

    @Test
    void shouldSeparateReadOnlyLowRiskWriteAndHighRiskPolicies() {
        assertThat(ToolRiskLevel.READ_ONLY.requiresApproval()).isFalse();
        assertThat(ToolRiskLevel.LOW_RISK_WRITE.requiresApproval()).isFalse();
        assertThat(ToolRiskLevel.HIGH_RISK.requiresApproval()).isTrue();
    }
}
