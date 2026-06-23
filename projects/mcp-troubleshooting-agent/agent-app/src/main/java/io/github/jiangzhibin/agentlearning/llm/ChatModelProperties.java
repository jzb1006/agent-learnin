package io.github.jiangzhibin.agentlearning.llm;

import java.net.URI;
import java.time.Duration;

/**
 * 模型调用配置。
 * <p>
 * 当前聚焦 OpenAI-compatible Chat API 所需的最小配置，并保持 API Key 与代码隔离。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:14:15
 */
public record ChatModelProperties(
    URI baseUrl,
    String apiKey,
    String modelName,
    Duration timeout
) {

    /**
     * 校验模型配置是否满足最小调用要求。
     */
    public ChatModelProperties {
        if (baseUrl == null) {
            throw new IllegalArgumentException("模型 baseUrl 不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("模型 apiKey 不能为空");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("模型 modelName 不能为空");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("模型 timeout 必须大于 0");
        }
    }

    /**
     * 返回默认 DeepSeek OpenAI-compatible 配置。
     *
     * @param apiKey DeepSeek API Key
     * @return 默认模型配置
     */
    public static ChatModelProperties deepSeek(String apiKey) {
        return new ChatModelProperties(
            URI.create("https://api.deepseek.com"),
            apiKey,
            "deepseek-v4-flash",
            Duration.ofSeconds(30)
        );
    }
}
