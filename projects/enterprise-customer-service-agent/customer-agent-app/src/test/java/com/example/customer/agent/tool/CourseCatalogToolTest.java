package com.example.customer.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tool.ToolResultStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseCatalogToolTest {

    private final CourseCatalogTool tool = new CourseCatalogTool(new MockCourseCatalogRepository());

    @Test
    void shouldExposeReadOnlyCourseCatalogDefinition() {
        var definition = tool.definition();

        assertThat(definition.name()).isEqualTo("course_catalog");
        assertThat(definition.riskLevel().name()).isEqualTo("READ_ONLY");
        assertThat(definition.permission().executionAllowed()).isTrue();
        assertThat(definition.requiredParameterNames()).containsExactlyElementsOf(List.of("tenantId"));
    }

    @Test
    void shouldReturnEnabledCatalogItemsForTenant() {
        var result = tool.list("tenant-demo", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("tenantId", "tenant-demo")
                .containsEntry("category", "ALL")
                .containsKey("items");
        assertThat(result.payload().get("items").toString())
                .contains("企业级 AI Agent 实战营")
                .contains("Spring AI RAG 进阶课")
                .doesNotContain("停用课程");
    }

    @Test
    void shouldFilterCatalogItemsByCategory() {
        var result = tool.list("tenant-demo", "PRODUCT");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload()).containsEntry("category", "PRODUCT");
        assertThat(result.payload().get("items").toString())
                .contains("企业级 AI Agent 实战营")
                .doesNotContain("退款政策");
    }

    @Test
    void shouldReturnInvalidArgumentWhenTenantIdIsBlank() {
        var result = tool.list(" ", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("INVALID_ARGUMENT");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("tenantId"));
    }

    @Test
    void shouldReturnNoCatalogItemsWhenTenantHasNoEnabledCatalog() {
        var result = tool.list("tenant-missing", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("CATALOG_NOT_FOUND");
    }
}
