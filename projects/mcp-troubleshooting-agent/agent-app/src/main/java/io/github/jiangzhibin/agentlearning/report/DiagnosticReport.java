package io.github.jiangzhibin.agentlearning.report;

import java.util.List;

/**
 * 结构化诊断报告。
 * <p>
 * 该对象是 Day 07 的输出契约，用于把模型文本稳定落到 Java 类型。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:57:18
 */
public record DiagnosticReport(
    String summary,
    List<String> evidence,
    List<String> nextActions,
    RiskLevel riskLevel
) {

    /**
     * 校验诊断报告必填字段。
     */
    public DiagnosticReport {
        requireText(summary, "summary");
        evidence = requireTextList(evidence, "evidence");
        nextActions = requireTextList(nextActions, "nextActions");
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel 不能为空");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
    }

    private static List<String> requireTextList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        values.forEach(value -> requireText(value, fieldName + " 元素"));
        return List.copyOf(values);
    }
}
