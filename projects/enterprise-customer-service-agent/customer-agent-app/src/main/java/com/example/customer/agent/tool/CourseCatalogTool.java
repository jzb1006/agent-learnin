package com.example.customer.agent.tool;

import com.example.customer.domain.knowledge.KnowledgeCategory;
import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 知识目录工具。
 * <p>
 * 查询指定租户可用课程和政策目录，为客服 Agent 提供低风险、只读的产品知识入口。
 *
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseCatalogTool {

    public static final String NAME = "course_catalog";
    public static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String ERROR_CATALOG_NOT_FOUND = "CATALOG_NOT_FOUND";

    private static final ToolDefinition DEFINITION = ToolDefinition.readOnly(
            NAME,
            "按租户查询可用课程和政策目录",
            List.of(
                    ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                    ToolParameterSchema.optional("category", ToolParameterType.STRING, "知识分类，可选 PRODUCT、POLICY、FAQ")));

    private final MockCourseCatalogRepository repository;

    /**
     * 返回知识目录工具定义。
     *
     * @return 工具定义
     */
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * 查询课程目录。
     *
     * @param tenantId 租户标识
     * @param category 知识分类，可为空
     * @return 工具结果
     */
    public ToolResult list(String tenantId, String category) {
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId");
        if (normalizedTenantId == null) {
            return invalidArgument("tenantId");
        }
        var normalizedCategory = normalizeCategory(category);
        if (normalizedCategory.invalid()) {
            return invalidArgument("category");
        }

        log.info(
                "tool_course_catalog_start tenantId={} category={}",
                normalizedTenantId,
                normalizedCategory.label());
        var items = repository.findEnabledByTenantId(normalizedTenantId, normalizedCategory.value());
        if (items.isEmpty()) {
            log.warn(
                    "tool_course_catalog_not_found tenantId={} category={}",
                    normalizedTenantId,
                    normalizedCategory.label());
            return ToolResult.failed(
                    NAME,
                    ERROR_CATALOG_NOT_FOUND,
                    "未找到可用课程目录: tenantId=%s, category=%s".formatted(normalizedTenantId, normalizedCategory.label()));
        }

        log.info(
                "tool_course_catalog_success tenantId={} category={} count={}",
                normalizedTenantId,
                normalizedCategory.label(),
                items.size());
        return ToolResult.succeeded(
                NAME,
                Map.of(
                        "tenantId", normalizedTenantId,
                        "category", normalizedCategory.label(),
                        "items", items.stream().map(this::toPayload).toList()));
    }

    private Map<String, Object> toPayload(CourseCatalogItem item) {
        return Map.of(
                "id", item.id(),
                "category", item.category().name(),
                "title", item.title(),
                "summary", item.summary());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("tool_course_catalog_invalid_argument field={}", fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private CategoryFilter normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return new CategoryFilter(null, "ALL", false);
        }
        var label = DomainText.requireNonBlank(category, "category").toUpperCase(Locale.ROOT);
        try {
            return new CategoryFilter(KnowledgeCategory.valueOf(label), label, false);
        } catch (IllegalArgumentException exception) {
            log.warn("tool_course_catalog_invalid_argument field=category value={}", category);
            return new CategoryFilter(null, label, true);
        }
    }

    private ToolResult invalidArgument(String fieldName) {
        return ToolResult.failed(NAME, ERROR_INVALID_ARGUMENT, "缺少或非法参数: " + fieldName);
    }

    private record CategoryFilter(KnowledgeCategory value, String label, boolean invalid) {
    }
}
