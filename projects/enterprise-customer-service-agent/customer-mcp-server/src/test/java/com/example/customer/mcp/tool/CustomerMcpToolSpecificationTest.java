package com.example.customer.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;

class CustomerMcpToolSpecificationTest {

    @Test
    void shouldGenerateMcpToolSpecificationsForP0Tools() {
        var specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(CustomerMcpTools.withDemoData()));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .containsExactlyInAnyOrder("kb_search", "order_lookup", "course_catalog", "refund_policy_check");
        assertThat(specifications)
                .filteredOn(specification -> specification.tool().name().equals("order_lookup"))
                .singleElement()
                .satisfies(specification -> {
                    assertThat(specification.tool().annotations().readOnlyHint()).isTrue();
                    assertThat(specification.tool().annotations().destructiveHint()).isFalse();
                    assertThat(specification.tool().inputSchema()).containsKey("properties");
                });
        assertThat(inputSchemaFor(specifications, "kb_search"))
                .extractingByKey("required")
                .asList()
                .containsExactlyInAnyOrder("tenantId", "query")
                .doesNotContain("topK");
        assertThat(inputSchemaFor(specifications, "course_catalog"))
                .extractingByKey("required")
                .asList()
                .containsExactly("tenantId")
                .doesNotContain("category");
    }

    @Test
    void shouldCallMcpToolSpecificationHandler() {
        var specification = SyncMcpAnnotationProviders.toolSpecifications(List.of(CustomerMcpTools.withDemoData()))
                .stream()
                .filter(candidate -> candidate.tool().name().equals("order_lookup"))
                .findFirst()
                .orElseThrow();

        var request = McpSchema.CallToolRequest.builder("order_lookup")
                .arguments(Map.of("tenantId", "tenant-demo", "orderId", "order-1001"))
                .build();
        var result = specification.callHandler().apply(null, request);

        assertThat(result.isError()).isFalse();
        assertThat(result.content())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(McpSchema.TextContent.class, content ->
                        assertThat(content.text())
                                .contains("\"toolName\":\"order_lookup\"")
                                .contains("\"orderId\":\"order-1001\""));
    }

    private Map<String, Object> inputSchemaFor(
            List<io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification> specifications,
            String toolName) {
        return specifications.stream()
                .filter(specification -> specification.tool().name().equals(toolName))
                .findFirst()
                .orElseThrow()
                .tool()
                .inputSchema();
    }
}
