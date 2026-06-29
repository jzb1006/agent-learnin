package com.example.customer.domain.tool;

import com.example.customer.domain.support.DomainText;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 工具定义。
 * <p>
 * 描述 Agent 可发现和可调用工具的稳定契约，不绑定具体 Spring AI 或 MCP 实现。
 *
 * @param name 工具名称
 * @param description 工具描述
 * @param parameters 参数 schema 列表
 * @param riskLevel 风险级别
 * @param permission 权限策略
 * @author jiangzhibin
 * @since 2026-06-29 10:40:00
 */
public record ToolDefinition(
        String name,
        String description,
        List<ToolParameterSchema> parameters,
        ToolRiskLevel riskLevel,
        ToolPermission permission) {

    /**
     * 创建只读工具定义。
     *
     * @param name 工具名称
     * @param description 工具描述
     * @param parameters 参数 schema 列表
     * @return 只读工具定义
     */
    public static ToolDefinition readOnly(
            String name,
            String description,
            List<ToolParameterSchema> parameters) {
        return new ToolDefinition(
                name,
                description,
                parameters,
                ToolRiskLevel.READ_ONLY,
                ToolPermission.allowReadOnly());
    }

    /**
     * 按工具名称查询参数定义。
     *
     * @param parameterName 参数名称
     * @return 参数定义
     */
    public Optional<ToolParameterSchema> parameter(String parameterName) {
        var normalizedName = DomainText.requireNonBlank(parameterName, "tool parameter name");
        return parameters.stream()
                .filter(parameter -> parameter.name().equals(normalizedName))
                .findFirst();
    }

    /**
     * 返回必填参数名称列表。
     *
     * @return 必填参数名称列表
     */
    public List<String> requiredParameterNames() {
        return parameters.stream()
                .filter(ToolParameterSchema::required)
                .map(ToolParameterSchema::name)
                .toList();
    }

    public ToolDefinition {
        name = DomainText.requireNonBlank(name, "tool name");
        description = DomainText.requireNonBlank(description, "tool description");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "tool parameters must not be null"));
        Objects.requireNonNull(riskLevel, "tool risk level must not be null");
        Objects.requireNonNull(permission, "tool permission must not be null");
        ensureUniqueParameterNames(parameters);
    }

    private static void ensureUniqueParameterNames(List<ToolParameterSchema> parameters) {
        var seen = new ArrayList<String>();
        for (var parameter : parameters) {
            Objects.requireNonNull(parameter, "tool parameter must not be null");
            if (seen.contains(parameter.name())) {
                throw new IllegalArgumentException("duplicate tool parameter: " + parameter.name());
            }
            seen.add(parameter.name());
        }
    }
}
