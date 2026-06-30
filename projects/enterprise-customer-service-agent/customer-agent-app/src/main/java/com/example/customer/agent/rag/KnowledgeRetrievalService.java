package com.example.customer.agent.rag;

import com.example.customer.domain.support.DomainText;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * 知识检索服务。
 * <p>
 * 负责把本地知识文档装载进 Spring AI VectorStore，并按租户执行语义检索。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeRetrievalService {

    private final VectorStore vectorStore;
    private final KnowledgeDocumentLoader documentLoader;
    private final KnowledgeDocumentSplitter documentSplitter;

    /**
     * 应用启动时建立本地知识索引。
     */
    @PostConstruct
    public void initializeIndex() {
        reindex();
    }

    /**
     * 重建本地知识索引。
     */
    public synchronized void reindex() {
        var documents = documentLoader.read();
        deleteExistingTenantKnowledge(documents);
        var chunks = documentSplitter.split(documents);
        if (chunks.isEmpty()) {
            log.warn("rag_knowledge_reindex_empty");
            return;
        }
        vectorStore.add(chunks);
        log.info("rag_knowledge_reindex_success documents={} chunks={}", documents.size(), chunks.size());
    }

    /**
     * 按租户检索知识。
     *
     * @param query 用户问题
     * @param tenantId 租户标识
     * @param topK 最大召回条数
     * @return 检索结果
     */
    public List<KnowledgeSearchResult> retrieve(String query, String tenantId, int topK) {
        var normalizedQuery = DomainText.requireNonBlank(query, "knowledge query");
        var normalizedTenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        var safeTopK = Math.max(1, topK);
        var filter = new FilterExpressionBuilder().eq("tenant", normalizedTenantId).build();
        var request = SearchRequest.builder()
                .query(normalizedQuery)
                .topK(safeTopK)
                .similarityThresholdAll()
                .filterExpression(filter)
                .build();
        var documents = vectorStore.similaritySearch(request);
        if (documents == null || documents.isEmpty()) {
            log.info("rag_knowledge_retrieve_empty tenantId={} queryLength={}", normalizedTenantId, normalizedQuery.length());
            return List.of();
        }
        var results = new ArrayList<KnowledgeSearchResult>();
        for (var document : documents) {
            results.add(toResult(document));
        }
        log.info(
                "rag_knowledge_retrieve_success tenantId={} queryLength={} count={}",
                normalizedTenantId,
                normalizedQuery.length(),
                results.size());
        return List.copyOf(results);
    }

    private void deleteExistingTenantKnowledge(List<Document> documents) {
        var tenants = documents.stream()
                .map(document -> document.getMetadata().get("tenant"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(tenant -> !tenant.isBlank())
                .distinct()
                .toList();
        var expressionBuilder = new FilterExpressionBuilder();
        for (var tenant : tenants) {
            vectorStore.delete(expressionBuilder.eq("tenant", tenant).build());
        }
    }

    private KnowledgeSearchResult toResult(Document document) {
        var metadata = document.getMetadata();
        return new KnowledgeSearchResult(
                metadataText(metadata.get("title")),
                metadataText(metadata.get("source")),
                metadataText(metadata.get("tenant")),
                metadataText(metadata.get("category")),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore());
    }

    private String metadataText(Object value) {
        return value == null ? "" : value.toString();
    }
}
