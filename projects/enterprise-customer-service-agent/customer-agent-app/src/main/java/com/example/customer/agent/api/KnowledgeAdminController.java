package com.example.customer.agent.api;

import com.example.customer.agent.knowledge.KnowledgeDeleteResponse;
import com.example.customer.agent.knowledge.KnowledgeItemRequest;
import com.example.customer.agent.knowledge.KnowledgeItemResponse;
import com.example.customer.agent.knowledge.KnowledgeItemSummaryResponse;
import com.example.customer.agent.knowledge.KnowledgeItemsResponse;
import com.example.customer.agent.knowledge.KnowledgeManagementService;
import com.example.customer.agent.knowledge.KnowledgeReindexResponse;
import com.example.customer.agent.knowledge.KnowledgeSearchMatchResponse;
import com.example.customer.agent.knowledge.KnowledgeSearchResponse;
import com.example.customer.agent.tenant.TenantContext;
import com.example.customer.domain.support.DomainText;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理侧 API。
 * <p>
 * 提供 Day 20 所需的知识增量索引、删除索引和显式重建索引能力，与用户侧对话 API 隔离。
 *
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
@RestController
@RequestMapping("/admin/api/v1/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeAdminController {

    private final KnowledgeManagementService managementService;

    /**
     * 列出当前运行态已索引的知识条目。
     *
     * @return 知识条目摘要列表
     */
    @GetMapping("/items")
    public KnowledgeItemsResponse listItems() {
        var tenantId = TenantContext.requireCurrentTenantId();
        log.info("knowledge_admin_api_list tenantId={}", tenantId);
        var items = managementService.listItems(tenantId).stream()
                .map(KnowledgeItemSummaryResponse::from)
                .toList();
        return new KnowledgeItemsResponse(items);
    }

    /**
     * 新增或更新知识条目。
     *
     * @param request 知识条目请求
     * @return 索引响应
     */
    @PostMapping("/items")
    public KnowledgeItemResponse upsertItem(@Valid @RequestBody KnowledgeItemRequest request) {
        var tenantId = TenantContext.requireCurrentTenantId();
        log.info("knowledge_admin_api_upsert tenantId={} itemId={}", tenantId, request.itemId());
        return KnowledgeItemResponse.from(managementService.upsertItem(tenantId, request));
    }

    /**
     * 删除知识条目索引。
     *
     * @param itemId 知识条目标识
     * @return 删除响应
     */
    @DeleteMapping("/items")
    public KnowledgeDeleteResponse deleteItem(@RequestParam("itemId") String itemId) {
        var tenantId = TenantContext.requireCurrentTenantId();
        var normalizedItemId = DomainText.requireNonBlank(itemId, "knowledge item id");
        log.info("knowledge_admin_api_delete tenantId={} itemId={}", tenantId, normalizedItemId);
        managementService.deleteItem(tenantId, normalizedItemId);
        return new KnowledgeDeleteResponse(normalizedItemId, tenantId, true);
    }

    /**
     * 搜索当前租户知识库。
     *
     * @param query 查询词
     * @param topK 最大召回条数
     * @return 搜索响应
     */
    @GetMapping("/search")
    public KnowledgeSearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(name = "topK", defaultValue = "3") int topK) {
        var tenantId = TenantContext.requireCurrentTenantId();
        var normalizedQuery = DomainText.requireNonBlank(query, "knowledge query");
        var safeTopK = Math.max(1, topK);
        log.info("knowledge_admin_api_search tenantId={} queryLength={} topK={}", tenantId, normalizedQuery.length(), safeTopK);
        var matches = managementService.search(tenantId, normalizedQuery, safeTopK).stream()
                .map(KnowledgeSearchMatchResponse::from)
                .toList();
        return new KnowledgeSearchResponse(normalizedQuery, tenantId, safeTopK, matches);
    }

    /**
     * 显式重建知识库索引。
     *
     * @return 重建响应
     */
    @PostMapping("/reindex")
    public KnowledgeReindexResponse reindex() {
        var tenantId = TenantContext.requireCurrentTenantId();
        log.info("knowledge_admin_api_reindex tenantId={}", tenantId);
        return KnowledgeReindexResponse.from(managementService.reindex());
    }
}
