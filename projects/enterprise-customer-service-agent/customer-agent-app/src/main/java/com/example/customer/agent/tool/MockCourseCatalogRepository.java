package com.example.customer.agent.tool;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 本地课程目录仓储。
 * <p>
 * 使用内存数据支撑 Day 13 工具调用闭环，后续接入知识库和 RAG 时再替换为持久化实现。
 *
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
@Repository
public class MockCourseCatalogRepository {

    private final List<CourseCatalogItem> items = List.of(
            new CourseCatalogItem(
                    "course-ai-agent",
                    "tenant-demo",
                    KnowledgeCategory.PRODUCT,
                    "企业级 AI Agent 实战营",
                    "面向 Java 后端工程师的企业客服 Agent 项目课程。",
                    true),
            new CourseCatalogItem(
                    "course-spring-ai-rag",
                    "tenant-demo",
                    KnowledgeCategory.PRODUCT,
                    "Spring AI RAG 进阶课",
                    "围绕 FAQ、政策和产品知识构建可引用的 RAG 检索链路。",
                    true),
            new CourseCatalogItem(
                    "policy-refund",
                    "tenant-demo",
                    KnowledgeCategory.POLICY,
                    "退款政策",
                    "用于回答退款、取消和改签前置政策咨询。",
                    true),
            new CourseCatalogItem(
                    "course-disabled",
                    "tenant-demo",
                    KnowledgeCategory.PRODUCT,
                    "停用课程",
                    "已下架课程，不应出现在目录工具返回中。",
                    false),
            new CourseCatalogItem(
                    "course-other-tenant",
                    "tenant-other",
                    KnowledgeCategory.PRODUCT,
                    "其他租户课程",
                    "用于验证租户隔离。",
                    true));

    /**
     * 查询指定租户下已启用的课程目录。
     *
     * @param tenantId 租户标识
     * @param category 可选知识分类
     * @return 目录条目列表
     */
    public List<CourseCatalogItem> findEnabledByTenantId(String tenantId, KnowledgeCategory category) {
        return items.stream()
                .filter(CourseCatalogItem::enabled)
                .filter(item -> item.tenantId().equals(tenantId))
                .filter(item -> category == null || item.category() == category)
                .toList();
    }
}
