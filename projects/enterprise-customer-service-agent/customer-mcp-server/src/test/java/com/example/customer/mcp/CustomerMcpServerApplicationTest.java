package com.example.customer.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.mcp.tool.CustomerMcpTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.main.web-application-type=none",
                "spring.ai.mcp.server.stdio=false"
        })
class CustomerMcpServerApplicationTest {

    @Autowired
    private CustomerMcpTools tools;

    @Test
    void shouldLoadMcpToolsBean() {
        assertThat(tools).isNotNull();
        assertThat(tools.orderLookup("tenant-demo", "order-1001").succeeded()).isTrue();
    }
}
