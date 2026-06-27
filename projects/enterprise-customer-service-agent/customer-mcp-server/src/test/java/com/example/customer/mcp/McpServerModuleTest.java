package com.example.customer.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpServerModuleTest {

    @Test
    void shouldExposeMcpServerModuleName() {
        assertThat(McpServerModule.name()).isEqualTo("customer-mcp-server");
    }
}
