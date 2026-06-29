package com.example.customer.agent.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KnowledgeDocumentLoaderTest {

    @TempDir
    private Path knowledgeBaseRoot;

    @Test
    void shouldLoadMarkdownFrontMatterAsDocumentMetadata() throws Exception {
        var documentPath = knowledgeBaseRoot.resolve("default/faq/learning-readiness.md");
        Files.createDirectories(documentPath.getParent());
        Files.writeString(knowledgeBaseRoot.resolve("README.md"), "# 知识库说明\n\n不是可检索知识文档。");
        Files.writeString(documentPath, """
                ---
                title: "课程适合哪些学员"
                source: "unit-test#Q1"
                tenant: "tenant-demo"
                version: "2026-06-29"
                category: "FAQ"
                tags:
                  - "beginner"
                  - "enterprise-agent"
                ---

                # 课程适合哪些学员

                课程面向有编程经验的开发者，不建议完全零基础学员直接参加。
                """);

        var documents = new KnowledgeDocumentLoader(knowledgeBaseRoot).read();

        assertThat(documents).hasSize(1);
        var document = documents.getFirst();
        assertThat(document.getText())
                .contains("课程面向有编程经验的开发者")
                .doesNotContain("source:")
                .doesNotContain("---");
        assertThat(document.getMetadata())
                .containsEntry("tenant", "tenant-demo")
                .containsEntry("source", "unit-test#Q1")
                .containsEntry("category", "FAQ")
                .containsEntry("title", "课程适合哪些学员")
                .containsEntry("version", "2026-06-29");
        assertThat(document.getMetadata().get("tags")).isEqualTo(List.of("beginner", "enterprise-agent"));
        assertThat(document.getMetadata().get("path").toString()).endsWith("default/faq/learning-readiness.md");
    }

    @Test
    void shouldSplitDocumentsAndPreserveRetrievalMetadata() throws Exception {
        var documentPath = knowledgeBaseRoot.resolve("default/policies/refund-policy.md");
        Files.createDirectories(documentPath.getParent());
        Files.writeString(documentPath, """
                ---
                title: "课程退款政策"
                source: "unit-test#Q93"
                tenant: "default"
                version: "2026-06-29"
                category: "POLICY"
                tags:
                  - "refund"
                ---

                # 课程退款政策

                开课一周内可无理由退款；超过一周后，需按已完成课时比例扣除费用。
                客服 Agent 只能解释退款政策并建议进入人工审批流程。
                """);
        var loader = new KnowledgeDocumentLoader(knowledgeBaseRoot);
        var splitter = new KnowledgeDocumentSplitter();

        var chunks = splitter.split(loader.read());

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getMetadata())
                .containsEntry("tenant", "default")
                .containsEntry("source", "unit-test#Q93")
                .containsEntry("category", "POLICY")
                .containsKey("chunkIndex"));
    }
}
