package com.example.customer.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.domain.tenant.Tenant;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerOrderTest {

    @Test
    void shouldKeepOrderInsideTenantBoundary() {
        var tenant = Tenant.active("tenant-education", "Education Business");
        var order = CustomerOrder.paid(
                "order-1001",
                tenant.id(),
                "customer-7",
                "Java AI Agent Course",
                Instant.parse("2026-06-27T00:00:00Z"));

        assertThat(order.belongsTo(tenant)).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.canBeExposedTo(tenant)).isTrue();
    }

    @Test
    void shouldRejectCrossTenantExposure() {
        var owner = Tenant.active("tenant-education", "Education Business");
        var anotherTenant = Tenant.active("tenant-finance", "Finance Business");
        var order = CustomerOrder.paid(
                "order-1001",
                owner.id(),
                "customer-7",
                "Java AI Agent Course",
                Instant.parse("2026-06-27T00:00:00Z"));

        assertThat(order.belongsTo(anotherTenant)).isFalse();
        assertThat(order.canBeExposedTo(anotherTenant)).isFalse();
    }

    @Test
    void shouldRejectBlankOrderIdentity() {
        assertThatThrownBy(() -> CustomerOrder.paid(
                        " ",
                        "tenant-education",
                        "customer-7",
                        "Java AI Agent Course",
                        Instant.parse("2026-06-27T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order id");
    }
}
