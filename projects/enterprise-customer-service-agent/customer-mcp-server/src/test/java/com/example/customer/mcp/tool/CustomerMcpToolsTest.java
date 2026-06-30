package com.example.customer.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tool.ToolResultStatus;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;

class CustomerMcpToolsTest {

    private final CustomerMcpTools tools = CustomerMcpTools.withDemoData();

    @Test
    void shouldAnnotateOnlyP0McpToolMethods() {
        var toolNames = Arrays.stream(CustomerMcpTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(McpTool.class))
                .map(method -> method.getAnnotation(McpTool.class).name())
                .sorted()
                .toList();

        assertThat(toolNames)
                .containsExactly("course_catalog", "kb_search", "order_lookup", "refund_policy_check")
                .doesNotContain("handoff_to_human");
    }

    @Test
    void kbSearchShouldReturnStructuredMatches() {
        var result = tools.kbSearch("tenant-demo", "退款政策", 2);

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.toolName()).isEqualTo("kb_search");
        assertThat(result.payload())
                .containsEntry("tenantId", "tenant-demo")
                .containsEntry("query", "退款政策");
        assertThat((List<?>) result.payload().get("matches")).isNotEmpty();
    }

    @Test
    void kbSearchShouldRejectBlankTenantId() {
        var result = tools.kbSearch(" ", "退款政策", 2);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.errorCode()).contains("INVALID_ARGUMENT");
    }

    @Test
    void orderLookupShouldHideOrdersFromOtherTenants() {
        var result = tools.orderLookup("tenant-other", "order-1001");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.errorCode()).contains("ORDER_NOT_FOUND");
    }

    @Test
    void courseCatalogShouldRejectUnknownCategory() {
        var result = tools.courseCatalog("tenant-demo", "SECRET");

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.errorCode()).contains("INVALID_ARGUMENT");
    }

    @Test
    void refundPolicyCheckShouldNeverExecuteFundOperation() {
        var result = tools.refundPolicyCheck("tenant-demo", "order-1001");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.toolName()).isEqualTo("refund_policy_check");
        assertThat(result.payload())
                .containsEntry("recommendedAction", "CREATE_APPROVAL_REQUEST")
                .containsEntry("fundOperationExecuted", false);
    }
}
