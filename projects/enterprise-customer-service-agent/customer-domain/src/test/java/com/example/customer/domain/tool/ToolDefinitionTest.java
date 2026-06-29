package com.example.customer.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToolDefinitionTest {

    @Test
    void shouldDescribeToolContractWithParametersRiskAndPermission() {
        var orderId = ToolParameterSchema.required(
                "orderId",
                ToolParameterType.STRING,
                "订单号");
        var tenantId = ToolParameterSchema.required(
                "tenantId",
                ToolParameterType.STRING,
                "租户 ID");
        var definition = ToolDefinition.readOnly(
                "order_lookup",
                "按订单号和租户查询订单状态",
                List.of(orderId, tenantId));

        assertThat(definition.name()).isEqualTo("order_lookup");
        assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.READ_ONLY);
        assertThat(definition.permission()).isEqualTo(ToolPermission.allowReadOnly());
        assertThat(definition.parameter("orderId")).contains(orderId);
        assertThat(definition.requiredParameterNames()).containsExactly("orderId", "tenantId");
    }

    @Test
    void shouldRejectDuplicateParameterNames() {
        var orderId = ToolParameterSchema.required(
                "orderId",
                ToolParameterType.STRING,
                "订单号");

        assertThatThrownBy(() -> ToolDefinition.readOnly(
                        "order_lookup",
                        "按订单号和租户查询订单状态",
                        List.of(orderId, orderId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate tool parameter");
    }

    @Test
    void shouldDerivePermissionFromRiskLevel() {
        assertThat(ToolPermission.defaultFor(ToolRiskLevel.READ_ONLY))
                .isEqualTo(ToolPermission.allowReadOnly());
        assertThat(ToolPermission.defaultFor(ToolRiskLevel.LOW_RISK_WRITE))
                .isEqualTo(ToolPermission.disabledLowRiskWrite());
        assertThat(ToolPermission.defaultFor(ToolRiskLevel.HIGH_RISK))
                .isEqualTo(ToolPermission.requireApproval());
    }
}
