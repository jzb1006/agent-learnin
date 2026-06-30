package com.example.customer.agent.knowledge;

import com.example.customer.agent.rag.KnowledgeIndexResult;
import com.example.customer.agent.rag.KnowledgeIndexedItem;
import com.example.customer.agent.rag.KnowledgeReindexResult;
import com.example.customer.agent.rag.KnowledgeRetrievalService;
import com.example.customer.agent.rag.KnowledgeSearchResult;
import com.example.customer.agent.rag.ManagedKnowledgeItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 知识库管理服务。
 * <p>
 * 将 API 请求转换为 RAG 索引操作，保持 Controller 不直接依赖向量库细节。
 *
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeManagementService {

    private final KnowledgeRetrievalService retrievalService;

    /**
     * 新增或更新单个知识条目索引。
     *
     * @param tenantId 租户标识
     * @param request 知识条目请求
     * @return 索引结果
     */
    public KnowledgeIndexResult upsertItem(String tenantId, KnowledgeItemRequest request) {
        var item = new ManagedKnowledgeItem(
                request.itemId(),
                tenantId,
                request.category(),
                request.title(),
                request.content(),
                request.source(),
                request.version(),
                request.tags());
        var result = retrievalService.indexItem(item);
        log.info(
                "knowledge_management_upsert tenantId={} itemId={} chunks={} skipped={}",
                result.tenantId(),
                result.itemId(),
                result.indexedChunks(),
                result.skipped());
        return result;
    }

    /**
     * 删除单个知识条目索引。
     *
     * @param tenantId 租户标识
     * @param itemId 知识条目标识
     */
    public void deleteItem(String tenantId, String itemId) {
        retrievalService.deleteItem(tenantId, itemId);
        log.info("knowledge_management_delete tenantId={} itemId={}", tenantId, itemId);
    }

    /**
     * 列出当前运行态已索引的知识条目。
     *
     * @param tenantId 租户标识
     * @return 知识条目摘要列表
     */
    public List<KnowledgeIndexedItem> listItems(String tenantId) {
        var items = retrievalService.listItems(tenantId);
        log.info("knowledge_management_list tenantId={} count={}", tenantId, items.size());
        return items;
    }

    /**
     * 搜索当前租户知识库。
     *
     * @param tenantId 租户标识
     * @param query 查询词
     * @param topK 最大召回条数
     * @return 搜索结果
     */
    public List<KnowledgeSearchResult> search(String tenantId, String query, int topK) {
        var matches = retrievalService.retrieve(query, tenantId, topK);
        log.info("knowledge_management_search tenantId={} queryLength={} count={}", tenantId, query.length(), matches.size());
        return matches;
    }

    /**
     * 显式重建本地知识库索引。
     *
     * @return 重建结果
     */
    public KnowledgeReindexResult reindex() {
        var result = retrievalService.reindex();
        log.info(
                "knowledge_management_reindex documents={} chunks={} skippedItems={}",
                result.documents(),
                result.indexedChunks(),
                result.skippedItems());
        return result;
    }
}
