package com.example.customer.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.rag.KnowledgeDocumentLoader;
import com.example.customer.agent.rag.KnowledgeDocumentSplitter;
import com.example.customer.agent.rag.KnowledgeRetrievalService;
import com.example.customer.agent.rag.LocalKnowledgeEmbeddingModel;
import com.example.customer.domain.tool.ToolResultStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.vectorstore.SimpleVectorStore;

class RetrieveKnowledgeToolTest {

    @TempDir
    private Path knowledgeBaseRoot;

    @Test
    void shouldExposeReadOnlyRetrieveKnowledgeDefinition() {
        var tool = retrieveKnowledgeTool();

        var definition = tool.definition();

        assertThat(definition.name()).isEqualTo("retrieve_knowledge");
        assertThat(definition.riskLevel().name()).isEqualTo("READ_ONLY");
        assertThat(definition.permission().executionAllowed()).isTrue();
        assertThat(definition.requiredParameterNames()).containsExactlyElementsOf(List.of("tenantId", "query"));
    }

    @Test
    void shouldReturnKnowledgeMatchesWithSources() throws Exception {
        writeKnowledge(
                "default/faq/learning-readiness.md",
                "课程适合哪些学员",
                "unit-test#Q1",
                "default",
                "FAQ",
                "课程面向有编程经验的开发者，不建议完全零基础学员直接参加。");
        var tool = retrieveKnowledgeTool();
        tool.reindex();

        var result = tool.search("新手适合学企业级 AI Agent 课程吗", "default", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.SUCCEEDED);
        assertThat(result.payload())
                .containsEntry("tenantId", "default")
                .containsEntry("query", "新手适合学企业级 AI Agent 课程吗")
                .containsKey("matches");
        assertThat(result.payload().get("matches").toString())
                .contains("课程适合哪些学员")
                .contains("unit-test#Q1")
                .contains("不建议完全零基础学员");
    }

    @Test
    void shouldReturnInvalidArgumentWhenQueryIsBlank() {
        var result = retrieveKnowledgeTool().search(" ", "default", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.errorCode()).hasValue("INVALID_ARGUMENT");
        assertThat(result.errorMessage()).hasValueSatisfying(message -> assertThat(message).contains("query"));
    }

    @Test
    void shouldReturnKnowledgeNotFoundWhenNoTenantMatch() throws Exception {
        writeKnowledge(
                "default/policies/refund-policy.md",
                "课程退款政策",
                "unit-test#Q93",
                "default",
                "POLICY",
                "开课一周内可无理由退款；超过一周后，需按已完成课时比例扣除费用。");
        var tool = retrieveKnowledgeTool();
        tool.reindex();

        var result = tool.search("退款政策是什么", "tenant-vip", null);

        assertThat(result.status()).isEqualTo(ToolResultStatus.FAILED);
        assertThat(result.payload()).isEmpty();
        assertThat(result.errorCode()).hasValue("KNOWLEDGE_NOT_FOUND");
    }

    private RetrieveKnowledgeTool retrieveKnowledgeTool() {
        return new RetrieveKnowledgeTool(new KnowledgeRetrievalService(
                SimpleVectorStore.builder(new LocalKnowledgeEmbeddingModel()).build(),
                new KnowledgeDocumentLoader(knowledgeBaseRoot),
                new KnowledgeDocumentSplitter()));
    }

    private void writeKnowledge(
            String relativePath,
            String title,
            String source,
            String tenant,
            String category,
            String content) throws Exception {
        var documentPath = knowledgeBaseRoot.resolve(relativePath);
        Files.createDirectories(documentPath.getParent());
        Files.writeString(documentPath, """
                ---
                title: "%s"
                source: "%s"
                tenant: "%s"
                version: "2026-06-29"
                category: "%s"
                tags:
                  - "test"
                ---

                # %s

                %s
                """.formatted(title, source, tenant, category, title, content));
    }
}
