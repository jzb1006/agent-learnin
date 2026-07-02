package com.example.customer.agent.security;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 敏感信息脱敏服务。
 * <p>
 * 统一处理进入日志、trace、Memory 和错误响应前的敏感文本，避免密码、身份证、银行卡和 token 明文扩散。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
@Component
public class RedactionService {

    private static final Pattern PASSWORD = Pattern.compile(
            "((?:密码)\\s*(?:是|为|:|=)\\s*|(?i:(?:password|passwd|pwd))\\s*(?:is|:|=)?\\s*)([^\\s,，。;；]+)");
    private static final Pattern AUTHORIZATION_BEARER =
            Pattern.compile("(?i)(Authorization\\s*:\\s*Bearer\\s+)([^\\s,，。;；]+)");
    private static final Pattern NAMED_TOKEN =
            Pattern.compile("(?i)((?:access_token|refresh_token|id_token|api_key|token)\\s*[:=]\\s*)([^\\s,，。;；]+)");
    private static final Pattern ID_CARD = Pattern.compile(
            "\\b[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b");
    private static final Pattern BANK_CARD = Pattern.compile("\\b\\d{16,19}\\b");

    /**
     * 对文本执行敏感信息脱敏。
     *
     * @param value 原始文本
     * @return 脱敏后的文本
     */
    public String redact(String value) {
        if (value == null) {
            return "";
        }
        var redacted = PASSWORD.matcher(value).replaceAll("$1[REDACTED_PASSWORD]");
        redacted = AUTHORIZATION_BEARER.matcher(redacted).replaceAll("$1[REDACTED_TOKEN]");
        redacted = NAMED_TOKEN.matcher(redacted).replaceAll("$1[REDACTED_TOKEN]");
        redacted = ID_CARD.matcher(redacted).replaceAll("[REDACTED_ID_CARD]");
        return BANK_CARD.matcher(redacted).replaceAll("[REDACTED_BANK_CARD]");
    }
}
