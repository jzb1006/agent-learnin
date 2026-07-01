package com.example.customer.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.domain.trace.ConversationRoute;
import org.junit.jupiter.api.Test;

class ConversationSummaryCompressorTest {

    @Test
    void shouldCompressLongUserMessageAndKeepRecentOrderContext() {
        var properties = new CustomerAgentProperties();
        properties.getConversationMemory().setMaxMessageChars(24);
        properties.getConversationMemory().setMaxSummaryChars(120);
        var compressor = new ConversationSummaryCompressor(properties);
        var longMessage = "帮我查询订单 order-1001 什么时候开课，"
                + "这里是一段很长的用户原文，".repeat(10)
                + "不要完整写入下一轮模型提示。";

        var summary = compressor.append("", "order-1001", longMessage, ConversationRoute.ORDER_LOOKUP);

        assertThat(summary).contains("最近订单 order-1001");
        assertThat(summary).contains("route=ORDER_LOOKUP");
        assertThat(summary).hasSizeLessThanOrEqualTo(120);
        assertThat(summary).doesNotContain("不要完整写入下一轮模型提示");
    }

    @Test
    void shouldPreferLatestSummaryWhenContextWindowIsFull() {
        var properties = new CustomerAgentProperties();
        properties.getConversationMemory().setMaxMessageChars(80);
        properties.getConversationMemory().setMaxSummaryChars(150);
        var compressor = new ConversationSummaryCompressor(properties);
        var summary = "";

        summary = compressor.append(summary, "order-1001", "第一轮很长的订单咨询 ".repeat(8), ConversationRoute.ORDER_LOOKUP);
        summary = compressor.append(summary, "order-1001", "刚才那个订单可以退款吗？", ConversationRoute.REFUND_OR_CANCEL);

        assertThat(summary).hasSizeLessThanOrEqualTo(150);
        assertThat(summary).contains("route=REFUND_OR_CANCEL");
        assertThat(summary).contains("刚才那个订单可以退款吗");
    }
}
