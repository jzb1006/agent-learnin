package com.example.customer.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.domain.tool.ToolResultStatus;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HandoffToHumanToolTest {

    private final MockHandoffRepository repository = new MockHandoffRepository();
    private final HandoffToHumanTool tool = new HandoffToHumanTool(repository);

    @Test
    void shouldExposeLowRiskWriteHandoffDefinition() {
        var definition = tool.definition();

        assertThat(definition.name()).isEqualTo("handoff_to_human");
        assertThat(definition.riskLevel().name()).isEqualTo("LOW_RISK_WRITE");
        assertThat(definition.permission().executionAllowed()).isFalse();
        assertThat(definition.permission().explicitEnableRequired()).isTrue();
        assertThat(definition.requiredParameterNames())
                .containsExactlyElementsOf(List.of("tenantId", "conversationId", "reason"));
    }

    @Test
    void shouldCreateLocalHandoffRecordWithoutExternalDispatch() {
        var result = tool.create("tenant-demo", "conversation-1001", "用户要求人工协助确认课程安排", "order-1001");

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("tenantId", "tenant-demo")
                .containsEntry("conversationId", "conversation-1001")
                .containsEntry("orderId", "order-1001")
                .containsEntry("status", "CREATED")
                .containsEntry("externalDispatch", false);
        assertThat(result.payload().get("handoffId").toString()).startsWith("handoff-");
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void shouldCreateTraceSummaryForDebugPanel() {
        var result = tool.create("tenant-demo", "conversation-1002", "模型无法确认用户诉求", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload()).containsKey("trace");
        assertThat(result.payload().get("trace").toString())
                .contains("handoff_to_human")
                .contains("LOW_RISK_WRITE")
                .contains("SUCCEEDED");
    }

    @Test
    void shouldReturnInvalidArgumentWhenReasonIsBlank() {
        var result = tool.create("tenant-demo", "conversation-1001", " ", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("INVALID_ARGUMENT");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("reason"));
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldKeepAllLocalHandoffRecordsWhenCreatedConcurrently() throws InterruptedException {
        var requestCount = 100;
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(requestCount);

        try (executor) {
            for (var i = 0; i < requestCount; i++) {
                var index = i;
                executor.submit(() -> {
                    try {
                        start.await();
                        tool.create(
                                "tenant-demo",
                                "conversation-" + index,
                                "并发转人工请求 " + index,
                                null);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(repository.findAll()).hasSize(requestCount);
    }
}
