package com.example.customer.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerAgentApplicationTest {

    @Test
    void shouldExposeApplicationModuleName() {
        assertThat(CustomerAgentApplication.moduleName()).isEqualTo("customer-agent-app");
    }
}
