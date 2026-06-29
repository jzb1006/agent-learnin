package com.example.customer.agent.tool;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import com.example.customer.domain.support.DomainText;
import java.util.Objects;

/**
 * 课程目录条目。
 * <p>
 * 表达 Agent 可返回给用户的产品目录摘要，不承载 RAG 切片或向量索引职责。
 *
 * @param id 目录条目标识
 * @param tenantId 租户标识
 * @param category 知识分类
 * @param title 课程标题
 * @param summary 课程摘要
 * @param enabled 是否启用
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
public record CourseCatalogItem(
        String id,
        String tenantId,
        KnowledgeCategory category,
        String title,
        String summary,
        boolean enabled) {

    public CourseCatalogItem {
        id = DomainText.requireNonBlank(id, "course catalog item id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        Objects.requireNonNull(category, "knowledge category must not be null");
        title = DomainText.requireNonBlank(title, "course catalog title");
        summary = DomainText.requireNonBlank(summary, "course catalog summary");
    }
}
