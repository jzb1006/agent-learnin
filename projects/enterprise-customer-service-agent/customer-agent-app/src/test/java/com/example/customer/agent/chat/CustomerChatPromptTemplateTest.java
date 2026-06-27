package com.example.customer.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerChatPromptTemplateTest {

    @Test
    void shouldExposeVersionedCustomerServicePromptContract() {
        var template = new CustomerChatPromptTemplate();

        assertThat(template.version()).isEqualTo("customer-agent-prompt-v1");
        assertThat(template.systemPrompt())
                .contains("不得编造订单状态")
                .contains("不得承诺真实退款成功")
                .contains("必须基于工具、知识库或用户输入证据")
                .contains("高风险动作必须进入审批");
    }

    @Test
    void shouldKeepBusinessFactsOutOfSystemPrompt() {
        var template = new CustomerChatPromptTemplate();

        assertThat(template.systemPrompt())
                .doesNotContain("order-1001")
                .doesNotContain("企业级 AI Agent 实战营")
                .doesNotContain("PAID");
    }

    @Test
    void shouldRenderUserPromptWithRuntimeEvidenceOnly() {
        var template = new CustomerChatPromptTemplate();
        var prompt = new CustomerChatPrompt(
                "tenant-demo",
                "帮我确认订单 order-9999 是否能退款",
                "订单 order-9999，课程「运行时证据课程」，状态 REFUND_REQUESTED");

        var rendered = template.userPrompt(prompt);

        assertThat(rendered)
                .contains("Prompt 版本：customer-agent-prompt-v1")
                .contains("租户：tenant-demo")
                .contains("用户问题：帮我确认订单 order-9999 是否能退款")
                .contains("事实证据：订单 order-9999，课程「运行时证据课程」，状态 REFUND_REQUESTED");
        assertThat(rendered)
                .doesNotContain("order-1001")
                .doesNotContain("企业级 AI Agent 实战营");
    }
}
