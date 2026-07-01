package com.example.customer.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.config.McpToolClientConfiguration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class McpToolOwnershipTest {

    @Test
    void shouldNotShipAgentAppLocalToolImplementations() {
        assertProductionClassAbsent("com.example.customer.agent.mcp.LocalMcpToolClient");
        assertProductionClassAbsent("com.example.customer.agent.tool.RetrieveKnowledgeTool");
        assertProductionClassAbsent("com.example.customer.agent.tool.OrderLookupTool");
        assertProductionClassAbsent("com.example.customer.agent.tool.CourseCatalogTool");
        assertProductionClassAbsent("com.example.customer.agent.tool.RefundPolicyCheckTool");
        assertProductionClassAbsent("com.example.customer.agent.tool.HandoffToHumanTool");
    }

    @Test
    void shouldConfigureMcpClientWithoutInjectingAgentAppToolClasses() {
        var method = Arrays.stream(McpToolClientConfiguration.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("mcpToolClient"))
                .findFirst()
                .orElseThrow();

        assertThat(method.getParameterTypes()).containsExactly(CustomerAgentProperties.class);
        assertThat(method.getReturnType()).isEqualTo(McpToolClient.class);
    }

    private void assertProductionClassAbsent(String className) {
        assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
