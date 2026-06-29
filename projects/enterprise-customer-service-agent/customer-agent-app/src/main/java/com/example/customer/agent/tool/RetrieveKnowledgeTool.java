package com.example.customer.agent.tool;

import com.example.customer.agent.rag.KnowledgeRetrievalService;
import com.example.customer.agent.rag.KnowledgeSearchResult;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 知识检索工具。
 * <p>
 * 通过 Spring AI VectorStore 在本地知识库中检索 FAQ、政策和产品知识，返回答案来源。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetrieveKnowledgeTool {

    public static final String NAME = "retrieve_knowledge";
    public static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String ERROR_KNOWLEDGE_NOT_FOUND = "KNOWLEDGE_NOT_FOUND";

    private static final int DEFAULT_TOP_K = 3;
    private static final ToolDefinition DEFINITION = ToolDefinition.readOnly(
            NAME,
            "按租户检索 FAQ、政策和产品知识",
            List.of(
                    ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                    ToolParameterSchema.required("query", ToolParameterType.STRING, "用户问题"),
                    ToolParameterSchema.optional("topK", ToolParameterType.NUMBER, "最大召回条数")));

    private final KnowledgeRetrievalService retrievalService;

    /**
     * 返回知识检索工具定义。
     *
     * @return 工具定义
     */
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * 重建底层知识索引。
     */
    public void reindex() {
        retrievalService.reindex();
    }

    /**
     * 检索知识库。
     *
     * @param query 用户问题
     * @param tenantId 租户标识
     * @param topK 最大召回条数，可为空
     * @return 工具结果
     */
    public ToolResult search(String query, String tenantId, Integer topK) {
        var normalizedQuery = normalize(query, "query");
        var normalizedTenantId = normalize(tenantId, "tenantId");
        if (normalizedQuery == null || normalizedTenantId == null) {
            return invalidArgument(normalizedQuery == null ? "query" : "tenantId");
        }
        var safeTopK = topK == null ? DEFAULT_TOP_K : Math.max(1, topK);

        log.info(
                "tool_retrieve_knowledge_start tenantId={} queryLength={} topK={}",
                normalizedTenantId,
                normalizedQuery.length(),
                safeTopK);
        var matches = retrievalService.retrieve(normalizedQuery, normalizedTenantId, safeTopK);
        if (matches.isEmpty()) {
            log.warn("tool_retrieve_knowledge_not_found tenantId={} queryLength={}",
                    normalizedTenantId,
                    normalizedQuery.length());
            return ToolResult.failed(
                    NAME,
                    ERROR_KNOWLEDGE_NOT_FOUND,
                    "未找到匹配知识: tenantId=%s".formatted(normalizedTenantId));
        }
        log.info("tool_retrieve_knowledge_success tenantId={} count={}", normalizedTenantId, matches.size());
        return ToolResult.succeeded(
                NAME,
                Map.of(
                        "tenantId", normalizedTenantId,
                        "query", normalizedQuery,
                        "matches", matches.stream().map(this::toPayload).toList()));
    }

    private Map<String, Object> toPayload(KnowledgeSearchResult result) {
        return Map.of(
                "title", result.title(),
                "source", result.source(),
                "tenant", result.tenant(),
                "category", result.category(),
                "content", result.content(),
                "score", result.score());
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("tool_retrieve_knowledge_invalid_argument field={}", fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private ToolResult invalidArgument(String fieldName) {
        return ToolResult.failed(NAME, ERROR_INVALID_ARGUMENT, "缺少或非法参数: " + fieldName);
    }
}
