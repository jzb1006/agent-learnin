package com.example.customer.domain.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tenant.Tenant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KnowledgeItemTest {

    @Test
    void shouldExposeEnabledKnowledgeOnlyToOwningTenant() {
        var owner = Tenant.active("tenant-education", "Education Business");
        var anotherTenant = Tenant.active("tenant-finance", "Finance Business");
        var item = KnowledgeItem.enabled(
                "kb-1",
                owner.id(),
                KnowledgeCategory.FAQ,
                "Can beginners learn this course?",
                "Yes, the course starts from Java backend fundamentals.",
                Set.of("beginner", "course"));

        assertThat(item.canBeRetrievedBy(owner)).isTrue();
        assertThat(item.canBeRetrievedBy(anotherTenant)).isFalse();
        assertThat(item.tags()).containsExactlyInAnyOrder("beginner", "course");
    }

    @Test
    void shouldHideDisabledKnowledgeItem() {
        var tenant = Tenant.active("tenant-education", "Education Business");
        var item = KnowledgeItem.disabled(
                "kb-1",
                tenant.id(),
                KnowledgeCategory.POLICY,
                "Refund policy",
                "Refund requires manual approval.",
                Set.of("refund"));

        assertThat(item.canBeRetrievedBy(tenant)).isFalse();
    }
}
