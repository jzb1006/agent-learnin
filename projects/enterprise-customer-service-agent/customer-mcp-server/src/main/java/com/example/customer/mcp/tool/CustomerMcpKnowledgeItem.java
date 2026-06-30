package com.example.customer.mcp.tool;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import com.example.customer.domain.support.DomainText;
import java.util.Objects;

/**
 * MCP 知识条目摘要。
 * <p>
 * 作为 Day 22 MCP Server 的只读 demo 知识源，后续可替换为共享知识查询用例。
 *
 * @param itemId 知识条目标识
 * @param tenantId 租户标识
 * @param category 知识分类
 * @param title 标题
 * @param source 来源
 * @param content 内容
 * @author jiangzhibin
 * @since 2026-06-30 16:56:00
 */
public record CustomerMcpKnowledgeItem(
        String itemId,
        String tenantId,
        KnowledgeCategory category,
        String title,
        String source,
        String content) {

    public CustomerMcpKnowledgeItem {
        itemId = DomainText.requireNonBlank(itemId, "knowledge item id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        Objects.requireNonNull(category, "knowledge category must not be null");
        title = DomainText.requireNonBlank(title, "knowledge title");
        source = DomainText.requireNonBlank(source, "knowledge source");
        content = DomainText.requireNonBlank(content, "knowledge content");
    }
}
