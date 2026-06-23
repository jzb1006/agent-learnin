package io.github.jiangzhibin.agentlearning.llm;

/**
 * 模型文本回复。
 * <p>
 * Day 06 只保留最小可验证字段，结构化诊断报告放到 Day 07。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:14:15
 */
public record ChatModelResponse(String content, String model) {

    /**
     * 校验模型回复是否可用。
     */
    public ChatModelResponse {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("模型回复内容不能为空");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
    }
}
