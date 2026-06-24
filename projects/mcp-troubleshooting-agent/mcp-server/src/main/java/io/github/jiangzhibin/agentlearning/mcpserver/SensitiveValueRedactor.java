package io.github.jiangzhibin.agentlearning.mcpserver;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感值脱敏工具。
 * <p>
 * 该类只做确定性文本替换，避免只读 MCP 工具把 token、secret、password 等值泄露给模型。
 *
 * @author jiangzhibin
 * @since 2026-06-24 11:48:00
 */
final class SensitiveValueRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
        Pattern.compile("(?i)\\b([\\w.-]*(?:password|passwd|pwd|secret|token|api[-_.]?key|access[-_.]?key|private[-_.]?key)[\\w.-]*\\s*[:=]\\s*)([^\\s,;]+)"),
        Pattern.compile("(?i)(\\b(?:password|passwd|pwd|secret|token|api[-_.]?key|access[-_.]?key|private[-_.]?key)\\b\\s+)([^\\s,;]+)")
    );

    private SensitiveValueRedactor() {
    }

    /**
     * 脱敏文本中的敏感值。
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        var redacted = text;
        for (var pattern : SENSITIVE_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll("$1" + REDACTED);
        }
        return redacted;
    }
}
