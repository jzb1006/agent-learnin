package com.example.customer.domain.trace;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Duration;
import java.util.Objects;

/**
 * 工具调用记录。
 * <p>
 * 保存单次工具调用的名称、风险、状态和耗时，供调试台和审计链路展示。
 *
 * @param toolName 工具名称
 * @param riskLevel 风险级别
 * @param status 调用状态
 * @param latency 调用耗时
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public record ToolCallRecord(
        String toolName,
        ToolRiskLevel riskLevel,
        ToolCallStatus status,
        Duration latency) {

    /**
     * 创建成功工具调用记录。
     *
     * @param toolName 工具名称
     * @param riskLevel 风险级别
     * @param latency 调用耗时
     * @return 成功工具调用记录
     */
    public static ToolCallRecord succeeded(String toolName, ToolRiskLevel riskLevel, Duration latency) {
        return new ToolCallRecord(toolName, riskLevel, ToolCallStatus.SUCCEEDED, latency);
    }

    public ToolCallRecord {
        toolName = DomainText.requireNonBlank(toolName, "tool name");
        Objects.requireNonNull(riskLevel, "tool risk level must not be null");
        Objects.requireNonNull(status, "tool call status must not be null");
        Objects.requireNonNull(latency, "tool latency must not be null");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("tool latency must not be negative");
        }
    }
}
