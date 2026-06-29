package com.example.customer.agent.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

/**
 * 知识文档切分器。
 * <p>
 * 委托 Spring AI TokenTextSplitter 切分正文，并补充 chunkIndex 供检索来源定位。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
public class KnowledgeDocumentSplitter {

    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    /**
     * 切分知识文档。
     *
     * @param documents 原始知识文档
     * @return 切分后的知识块
     */
    public List<Document> split(List<Document> documents) {
        var chunks = textSplitter.split(documents);
        var counters = new LinkedHashMap<String, Integer>();
        var enriched = new ArrayList<Document>();
        for (var chunk : chunks) {
            var metadata = new LinkedHashMap<String, Object>(chunk.getMetadata());
            var sourceKey = metadata.getOrDefault("path", metadata.getOrDefault("source", chunk.getId())).toString();
            var chunkIndex = counters.merge(sourceKey, 1, Integer::sum) - 1;
            metadata.put("chunkIndex", chunkIndex);
            enriched.add(new Document(chunk.getId(), chunk.getText(), metadata));
        }
        return List.copyOf(enriched);
    }
}
