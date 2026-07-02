package com.example.customer.agent.security;

import java.util.List;

/**
 * Prompt Injection 检查结果。
 *
 * @param safe 是否可继续执行
 * @param riskLevel 检测风险等级
 * @param reasons 风险原因
 * @param sanitizedMessage 脱敏且移除危险片段后的消息
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public record PromptInspectionResult(
        boolean safe,
        String riskLevel,
        List<String> reasons,
        String sanitizedMessage) {

    /**
     * 创建检查结果。
     */
    public PromptInspectionResult {
        reasons = List.copyOf(reasons);
        sanitizedMessage = sanitizedMessage == null ? "" : sanitizedMessage;
    }
}
