package com.example.customer.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantTest {

    @Test
    void shouldCreateActiveTenantWithRequiredIdentity() {
        var tenant = Tenant.active("tenant-education", "Education Business");

        assertThat(tenant.id()).isEqualTo("tenant-education");
        assertThat(tenant.name()).isEqualTo("Education Business");
        assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.isActive()).isTrue();
    }

    @Test
    void shouldRejectBlankTenantIdentity() {
        assertThatThrownBy(() -> Tenant.active(" ", "Education Business"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant id");
    }
}
