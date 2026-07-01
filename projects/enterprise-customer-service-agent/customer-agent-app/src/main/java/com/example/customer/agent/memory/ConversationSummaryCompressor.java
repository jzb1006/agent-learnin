package com.example.customer.agent.memory;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.domain.trace.ConversationRoute;
import org.springframework.stereotype.Component;

/**
 * 会话摘要压缩器。
 * <p>
 * 当前采用确定性裁剪策略：规范化空白、裁剪单轮消息、拼接业务摘要，再保留最新窗口。
 *
 * @author jiangzhibin
 * @since 2026-07-01 16:20:00
 */
@Component
public class ConversationSummaryCompressor {

    private final CustomerAgentProperties properties;

    /**
     * 创建会话摘要压缩器。
     *
     * @param properties 客服 Agent 配置
     */
    public ConversationSummaryCompressor(CustomerAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 追加一轮对话摘要，并压缩到配置的最大长度。
     *
     * @param existingSummary 已有摘要
     * @param orderId 最近订单号
     * @param userMessage 用户消息
     * @param route 路由结果
     * @return 压缩后的摘要
     */
    public String append(String existingSummary, String orderId, String userMessage, ConversationRoute route) {
        var conversationMemory = properties.getConversationMemory();
        var currentOrder = orderId == null || orderId.isBlank() ? "无订单上下文" : "最近订单 " + orderId;
        var line = "%s；route=%s；用户=%s".formatted(
                currentOrder,
                route.name(),
                trim(userMessage, conversationMemory.getMaxMessageChars(), true));
        return trim(joinSummary(existingSummary, line), conversationMemory.getMaxSummaryChars(), false);
    }

    private String joinSummary(String existing, String line) {
        if (existing == null || existing.isBlank()) {
            return line;
        }
        return existing + " | " + line;
    }

    private String trim(String value, int maxChars, boolean keepPrefix) {
        var normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        if (maxChars <= 0 || normalized.length() <= maxChars) {
            return normalized;
        }
        if (keepPrefix) {
            return normalized.substring(0, Math.max(0, maxChars - 1)) + "…";
        }
        return "…" + normalized.substring(normalized.length() - Math.max(0, maxChars - 1));
    }
}
