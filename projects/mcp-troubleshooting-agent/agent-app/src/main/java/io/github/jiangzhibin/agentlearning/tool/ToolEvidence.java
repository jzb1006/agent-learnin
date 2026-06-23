package io.github.jiangzhibin.agentlearning.tool;

/**
 * 工具返回的证据片段。
 * <p>
 * 证据必须带来源，避免排障报告把模型推断误当成事实。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:24:42
 */
public record ToolEvidence(String source, String content) {

    /**
     * 校验证据来源和内容。
     */
    public ToolEvidence {
        requireText(source, "证据来源");
        requireText(content, "证据内容");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }
}
