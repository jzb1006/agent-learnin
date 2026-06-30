package com.example.customer.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tool.ToolRiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerMcpToolCatalogTest {

    @Test
    void shouldExposeOnlyReadonlyP0McpToolsByDefault() {
        var catalog = CustomerMcpToolCatalog.p0();

        assertThat(catalog.toolNames())
                .containsExactly("kb_search", "order_lookup", "course_catalog", "refund_policy_check")
                .doesNotContain("handoff_to_human");
        assertThat(catalog.definitions())
                .allSatisfy(definition -> {
                    assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.READ_ONLY);
                    assertThat(definition.permission().executionAllowed()).isTrue();
                    assertThat(definition.permission().approvalRequired()).isFalse();
                });
    }

    @Test
    void shouldKeepKbSearchSchemaTenantScoped() {
        var kbSearch = CustomerMcpToolCatalog.p0().requireDefinition("kb_search");

        assertThat(kbSearch.requiredParameterNames()).containsExactly("tenantId", "query");
        assertThat(kbSearch.parameter("topK")).isPresent();
    }

    @Test
    void shouldRejectUnknownToolName() {
        var catalog = CustomerMcpToolCatalog.p0();

        assertThat(catalog.findDefinition("handoff_to_human")).isEmpty();
    }

    @Test
    void shouldReturnImmutableDefinitions() {
        var catalog = CustomerMcpToolCatalog.p0();

        assertThat(catalog.definitions())
                .isUnmodifiable()
                .extracting("name")
                .isEqualTo(List.of("kb_search", "order_lookup", "course_catalog", "refund_policy_check"));
    }
}
