package com.example.customer.agent.rag;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import com.example.customer.domain.support.DomainText;
import java.util.List;
import java.util.Locale;

/**
 * 运行时可管理知识条目。
 * <p>
 * 表达通过知识库管理 API 增量写入向量索引的最小知识单元。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param category 知识分类
 * @param title 标题
 * @param content 正文
 * @param source 来源
 * @param version 版本
 * @param tags 标签
 * @author jiangzhibin
 * @since 2026-06-30 16:20:00
 */
public record ManagedKnowledgeItem(
        String itemId,
        String tenantId,
        String category,
        String title,
        String content,
        String source,
        String version,
        List<String> tags) {

    public ManagedKnowledgeItem {
        itemId = DomainText.requireNonBlank(itemId, "knowledge item id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        category = normalizeCategory(category);
        title = DomainText.requireNonBlank(title, "knowledge title");
        content = DomainText.requireNonBlank(content, "knowledge content");
        source = source == null || source.isBlank() ? "knowledge-api:" + itemId : source.strip();
        version = version == null || version.isBlank() ? "runtime" : version.strip();
        tags = normalizeTags(tags);
    }

    private static String normalizeCategory(String value) {
        var normalized = DomainText.requireNonBlank(value, "knowledge category").toUpperCase(Locale.ROOT);
        KnowledgeCategory.valueOf(normalized);
        return normalized;
    }

    private static List<String> normalizeTags(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }
}
