package com.example.customer.domain.knowledge;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tenant.Tenant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库条目。
 * <p>
 * 表达 FAQ、政策或产品知识的租户归属和检索状态。向量、切片和索引细节由后续 RAG 模块负责。
 *
 * @param id 知识条目唯一标识
 * @param tenantId 归属租户标识
 * @param category 知识分类
 * @param title 标题
 * @param content 正文
 * @param tags 标签集合
 * @param status 知识状态
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record KnowledgeItem(
        String id,
        String tenantId,
        KnowledgeCategory category,
        String title,
        String content,
        Set<String> tags,
        KnowledgeStatus status) {

    /**
     * 创建启用知识条目。
     *
     * @param id 知识条目唯一标识
     * @param tenantId 归属租户标识
     * @param category 知识分类
     * @param title 标题
     * @param content 正文
     * @param tags 标签集合
     * @return 启用知识条目
     */
    public static KnowledgeItem enabled(
            String id,
            String tenantId,
            KnowledgeCategory category,
            String title,
            String content,
            Set<String> tags) {
        return new KnowledgeItem(id, tenantId, category, title, content, tags, KnowledgeStatus.ENABLED);
    }

    /**
     * 创建停用知识条目。
     *
     * @param id 知识条目唯一标识
     * @param tenantId 归属租户标识
     * @param category 知识分类
     * @param title 标题
     * @param content 正文
     * @param tags 标签集合
     * @return 停用知识条目
     */
    public static KnowledgeItem disabled(
            String id,
            String tenantId,
            KnowledgeCategory category,
            String title,
            String content,
            Set<String> tags) {
        return new KnowledgeItem(id, tenantId, category, title, content, tags, KnowledgeStatus.DISABLED);
    }

    public KnowledgeItem {
        id = DomainText.requireNonBlank(id, "knowledge item id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        Objects.requireNonNull(category, "knowledge category must not be null");
        title = DomainText.requireNonBlank(title, "knowledge title");
        content = DomainText.requireNonBlank(content, "knowledge content");
        tags = copyTags(tags);
        Objects.requireNonNull(status, "knowledge status must not be null");
    }

    /**
     * 判断知识条目是否可被指定租户检索。
     *
     * @param tenant 租户
     * @return 启用且归属匹配时返回 true
     */
    public boolean canBeRetrievedBy(Tenant tenant) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        return status == KnowledgeStatus.ENABLED && tenant.isActive() && tenantId.equals(tenant.id());
    }

    private static Set<String> copyTags(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return source.stream()
                .map(tag -> DomainText.requireNonBlank(tag, "knowledge tag"))
                .collect(Collectors.toUnmodifiableSet());
    }
}
