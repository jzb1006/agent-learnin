package com.example.customer.agent.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

class KnowledgeRetrievalServiceTest {

    @TempDir
    private Path knowledgeBaseRoot;

    @Test
    void shouldRetrieveTenantScopedFaqWithSources() throws Exception {
        writeKnowledge(
                "default/faq/learning-readiness.md",
                "课程适合哪些学员",
                "unit-test#Q1",
                "default",
                "FAQ",
                """
                课程面向有编程经验的开发者、架构师和技术管理者。
                不建议完全零基础学员直接参加。
                课程强调企业级 AI Agent 落地和可运行代码。
                """);
        writeKnowledge(
                "tenant-vip/faq/learning-readiness.md",
                "VIP 课程适合哪些学员",
                "unit-test#VIP-Q1",
                "tenant-vip",
                "FAQ",
                """
                VIP 课程只面向已有企业项目和团队管理经验的学员。
                """);
        var service = new KnowledgeRetrievalService(
                SimpleVectorStore.builder(new LocalKnowledgeEmbeddingModel()).build(),
                new KnowledgeDocumentLoader(knowledgeBaseRoot),
                new KnowledgeDocumentSplitter());
        service.reindex();

        var results = service.retrieve("新手适合学企业级 AI Agent 课程吗", "default", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().title()).isEqualTo("课程适合哪些学员");
        assertThat(results.getFirst().source()).isEqualTo("unit-test#Q1");
        assertThat(results.getFirst().tenant()).isEqualTo("default");
        assertThat(results.getFirst().category()).isEqualTo("FAQ");
        assertThat(results.getFirst().content()).contains("不建议完全零基础学员");
        assertThat(results).noneSatisfy(result -> assertThat(result.source()).isEqualTo("unit-test#VIP-Q1"));
    }

    @Test
    void shouldReturnEmptyWhenTenantDoesNotOwnMatchingKnowledge() throws Exception {
        writeKnowledge(
                "default/policies/refund-policy.md",
                "课程退款政策",
                "unit-test#Q93",
                "default",
                "POLICY",
                "开课一周内可无理由退款；超过一周后，需按已完成课时比例扣除费用。");
        var service = new KnowledgeRetrievalService(
                SimpleVectorStore.builder(new LocalKnowledgeEmbeddingModel()).build(),
                new KnowledgeDocumentLoader(knowledgeBaseRoot),
                new KnowledgeDocumentSplitter());
        service.reindex();

        var results = service.retrieve("退款政策是什么", "tenant-vip", 3);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldDeleteExistingKnowledgeItemChunksBeforeReindex() throws Exception {
        writeKnowledge(
                "default/faq/learning-readiness.md",
                "课程适合哪些学员",
                "unit-test#Q1",
                "default",
                "FAQ",
                "课程面向有编程经验的开发者。");
        var vectorStore = new RecordingVectorStore();
        var service = new KnowledgeRetrievalService(
                vectorStore, new KnowledgeDocumentLoader(knowledgeBaseRoot), new KnowledgeDocumentSplitter());

        service.reindex();

        assertThat(vectorStore.deletedExpression).isNotNull();
        assertThat(vectorStore.deletedExpression.type()).isEqualTo(Filter.ExpressionType.AND);
        assertThat(vectorStore.deletedExpression.toString())
                .contains("tenant")
                .contains("default")
                .contains("itemId")
                .contains("default/faq/learning-readiness.md");
        assertThat(vectorStore.addedDocuments).isNotEmpty();
    }

    @Test
    void shouldNotReindexAutomaticallyWhenServiceIsConstructed() {
        var vectorStore = new RecordingVectorStore();

        new KnowledgeRetrievalService(
                vectorStore, new KnowledgeDocumentLoader(knowledgeBaseRoot), new KnowledgeDocumentSplitter());

        assertThat(vectorStore.addCalls).isZero();
        assertThat(vectorStore.deleteCalls).isZero();
    }

    @Test
    void shouldAddAndDeleteSingleManagedKnowledgeItem() {
        var service = new KnowledgeRetrievalService(
                SimpleVectorStore.builder(new LocalKnowledgeEmbeddingModel()).build(),
                new KnowledgeDocumentLoader(knowledgeBaseRoot),
                new KnowledgeDocumentSplitter());

        var addResult = service.indexItem(
                new ManagedKnowledgeItem(
                        "faq-day20",
                        "tenant-demo",
                        "FAQ",
                        "Day20 知识管理 API",
                        "新增知识无需重启服务即可被检索。",
                        "day20#faq",
                        "2026-06-30",
                        List.of("day20")));
        var results = service.retrieve("新增知识是否需要重启", "tenant-demo", 3);
        service.deleteItem("tenant-demo", "faq-day20");
        var deletedResults = service.retrieve("新增知识是否需要重启", "tenant-demo", 3);

        assertThat(addResult.indexedChunks()).isPositive();
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().title()).isEqualTo("Day20 知识管理 API");
        assertThat(results.getFirst().source()).isEqualTo("day20#faq");
        assertThat(deletedResults).isEmpty();
    }

    @Test
    void shouldSkipUnchangedKnowledgeItemWhenReindexing() {
        var vectorStore = new RecordingVectorStore();
        var service = new KnowledgeRetrievalService(
                vectorStore, new KnowledgeDocumentLoader(knowledgeBaseRoot), new KnowledgeDocumentSplitter());
        var item = new ManagedKnowledgeItem(
                "faq-day20",
                "tenant-demo",
                "FAQ",
                "Day20 知识管理 API",
                "知识内容未变化时不重复生成相同向量。",
                "day20#faq",
                "2026-06-30",
                List.of("day20"));

        var first = service.indexItem(item);
        var second = service.indexItem(item);

        assertThat(first.skipped()).isFalse();
        assertThat(second.skipped()).isTrue();
        assertThat(vectorStore.addCalls).isEqualTo(1);
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

    private static class RecordingVectorStore implements VectorStore {

        private Filter.Expression deletedExpression;
        private List<Document> addedDocuments = List.of();
        private int addCalls;
        private int deleteCalls;

        @Override
        public void add(List<Document> documents) {
            addCalls++;
            addedDocuments = List.copyOf(documents);
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            deleteCalls++;
            deletedExpression = filterExpression;
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return new ArrayList<>();
        }
    }
}
