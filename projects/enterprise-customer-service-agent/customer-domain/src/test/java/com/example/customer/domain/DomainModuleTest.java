package com.example.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainModuleTest {

    @Test
    void shouldExposeDomainModuleName() {
        assertThat(DomainModule.name()).isEqualTo("customer-domain");
    }
}
