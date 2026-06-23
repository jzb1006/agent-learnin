package io.github.jiangzhibin.agentlearning.llm;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DeepSeekChatModelClientSmokeTest {

    @Test
    void completeShouldCallRealDeepSeekApiWhenExplicitlyEnabled() {
        assumeTrue(Boolean.getBoolean("deepseek.smoke"), "需要显式传入 -Ddeepseek.smoke=true 才调用真实 API");

        var apiKey = Optional.ofNullable(System.getenv("DEEPSEEK_API_KEY"))
            .filter(value -> !value.isBlank());
        assumeTrue(apiKey.isPresent(), "需要先配置 DEEPSEEK_API_KEY");

        var client = new OpenAiCompatibleChatModelClient(ChatModelProperties.deepSeek(apiKey.get()));

        var response = client.complete("用一句中文回答：你已收到 Day 06 的最小 LLM API 调用测试。");

        assertFalse(response.content().isBlank());
    }
}
