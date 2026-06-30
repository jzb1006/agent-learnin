package com.example.customer.agent.rag;

import com.example.customer.domain.support.DomainText;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, String> indexedFingerprints = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeIndexedItem> indexedItems = new ConcurrentHashMap<>();

    /**
     * 显式重建知识索引。
     *
     * @return 重建结果
     */
    public synchronized KnowledgeReindexResult reindex() {
        var documents = documentLoader.read();
        if (documents.isEmpty()) {
            log.warn("rag_knowledge_reindex_empty");
            return new KnowledgeReindexResult(0, 0, 0);
        }
        var indexedChunks = 0;
        var skippedItems = 0;
        for (var document : documents) {
            var result = indexDocument(document);
            indexedChunks += result.indexedChunks();
            if (result.skipped()) {
                skippedItems++;
            }
        }
        log.info(
                "rag_knowledge_reindex_success documents={} chunks={} skippedItems={}",
                documents.size(),
                indexedChunks,
                skippedItems);
        return new KnowledgeReindexResult(documents.size(), indexedChunks, skippedItems);
    }

    /**
     * 增量索引单个运行时知识条目。
     *
     * @param item 知识条目
     * @return 索引结果
     */
    public synchronized KnowledgeIndexResult indexItem(ManagedKnowledgeItem item) {
        return indexDocument(documentFrom(item));
    }

    /**
     * 删除单个知识条目的索引。
     *
     * @param tenantId 租户标识
     * @param itemId 知识条目标识
     */
    public synchronized void deleteItem(String tenantId, String itemId) {
        var normalizedTenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        var normalizedItemId = DomainText.requireNonBlank(itemId, "knowledge item id");
        deleteItemChunks(normalizedTenantId, normalizedItemId);
        indexedFingerprints.remove(fingerprintKey(normalizedTenantId, normalizedItemId));
        indexedItems.remove(fingerprintKey(normalizedTenantId, normalizedItemId));
        log.info("rag_knowledge_delete_success tenantId={} itemId={}", normalizedTenantId, normalizedItemId);
    }

    /**
     * 列出当前服务生命周期内已索引的知识条目。
     *
     * @param tenantId 租户标识
     * @return 知识条目摘要列表
     */
    public List<KnowledgeIndexedItem> listItems(String tenantId) {
        var normalizedTenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        return indexedItems.values().stream()
                .filter(item -> normalizedTenantId.equals(item.tenantId()))
                .sorted((left, right) -> left.itemId().compareTo(right.itemId()))
                .toList();
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

    private KnowledgeIndexResult indexDocument(Document document) {
        var metadata = document.getMetadata();
        var tenantId = requiredMetadata(metadata, "tenant");
        var itemId = requiredMetadata(metadata, "itemId");
        var fingerprint = fingerprint(document);
        var fingerprintKey = fingerprintKey(tenantId, itemId);
        if (fingerprint.equals(indexedFingerprints.get(fingerprintKey))) {
            log.info("rag_knowledge_index_skipped tenantId={} itemId={}", tenantId, itemId);
            return new KnowledgeIndexResult(itemId, tenantId, 0, true);
        }
        deleteItemChunks(tenantId, itemId);
        var chunks = documentSplitter.split(List.of(document));
        if (chunks.isEmpty()) {
            log.warn("rag_knowledge_index_empty tenantId={} itemId={}", tenantId, itemId);
            indexedFingerprints.remove(fingerprintKey);
            indexedItems.remove(fingerprintKey);
            return new KnowledgeIndexResult(itemId, tenantId, 0, false);
        }
        vectorStore.add(chunks);
        indexedFingerprints.put(fingerprintKey, fingerprint);
        indexedItems.put(fingerprintKey, indexedItem(document, chunks.size()));
        log.info("rag_knowledge_index_success tenantId={} itemId={} chunks={}", tenantId, itemId, chunks.size());
        return new KnowledgeIndexResult(itemId, tenantId, chunks.size(), false);
    }

    private Document documentFrom(ManagedKnowledgeItem item) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("itemId", item.itemId());
        metadata.put("tenant", item.tenantId());
        metadata.put("category", item.category());
        metadata.put("title", item.title());
        metadata.put("source", item.source());
        metadata.put("version", item.version());
        metadata.put("tags", item.tags());
        metadata.put("path", "api/" + item.tenantId() + "/" + item.itemId());
        return new Document(item.itemId(), item.content(), metadata);
    }

    private void deleteItemChunks(String tenantId, String itemId) {
        var expressionBuilder = new FilterExpressionBuilder();
        var expression = expressionBuilder
                .and(expressionBuilder.eq("tenant", tenantId), expressionBuilder.eq("itemId", itemId))
                .build();
        vectorStore.delete(expression);
    }

    private String requiredMetadata(Map<String, Object> metadata, String key) {
        var value = metadata.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("知识文档缺少必要元数据: " + key);
        }
        return value.toString();
    }

    private String fingerprint(Document document) {
        var metadata = document.getMetadata();
        var raw = String.join(
                "\n",
                requiredMetadata(metadata, "tenant"),
                requiredMetadata(metadata, "itemId"),
                metadataText(metadata.get("category")),
                metadataText(metadata.get("title")),
                metadataText(metadata.get("source")),
                metadataText(metadata.get("version")),
                metadataText(metadata.get("tags")),
                document.getText());
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private String fingerprintKey(String tenantId, String itemId) {
        return tenantId + ":" + itemId;
    }

    private KnowledgeSearchResult toResult(Document document) {
        var metadata = document.getMetadata();
        return new KnowledgeSearchResult(
                metadataText(metadata.get("itemId")),
                metadataText(metadata.get("title")),
                metadataText(metadata.get("source")),
                metadataText(metadata.get("tenant")),
                metadataText(metadata.get("category")),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore());
    }

    private KnowledgeIndexedItem indexedItem(Document document, int indexedChunks) {
        var metadata = document.getMetadata();
        return new KnowledgeIndexedItem(
                requiredMetadata(metadata, "itemId"),
                requiredMetadata(metadata, "tenant"),
                metadataText(metadata.get("category")),
                metadataText(metadata.get("title")),
                metadataText(metadata.get("source")),
                metadataText(metadata.get("version")),
                indexedChunks,
                contentPreview(document.getText()));
    }

    private String contentPreview(String content) {
        if (content == null || content.length() <= 120) {
            return content == null ? "" : content;
        }
        return content.substring(0, 120);
    }

    private String metadataText(Object value) {
        return value == null ? "" : value.toString();
    }
}
