package com.example.customer.agent.intent;

import com.example.customer.domain.trace.ConversationRoute;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 客服意图路由器。
 * <p>
 * Day 08 只实现本地确定性 fallback：先用关键词覆盖典型客服入口，后续再接模型结构化输出。
 *
 * @author jiangzhibin
 * @since 2026-06-27 15:40:00
 */
@Component
public class IntentRouter {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("order-[0-9]+", Pattern.CASE_INSENSITIVE);

    /**
     * 根据用户消息生成意图路由。
     *
     * @param message 用户消息
     * @return 意图路由结果
     */
    public IntentRouteResult route(String message) {
        var normalizedMessage = Optional.ofNullable(message).orElse("").strip().toLowerCase();
        var orderId = extractOrderId(normalizedMessage).orElse(null);

        if (containsAny(normalizedMessage, "退款", "退费", "取消", "改签")) {
            return new IntentRouteResult(
                    ConversationRoute.REFUND_OR_CANCEL,
                    orderId,
                    0.90,
                    "命中退款/取消/改签关键词，需要进入高风险动作前置判断");
        }
        if (containsAny(normalizedMessage, "人工", "转接", "真人", "投诉")) {
            return new IntentRouteResult(
                    ConversationRoute.HUMAN_HANDOFF,
                    orderId,
                    0.88,
                    "命中人工转接关键词");
        }
        if (orderId != null || containsAny(normalizedMessage, "订单", "开课", "物流", "发票", "支付")) {
            return new IntentRouteResult(
                    ConversationRoute.ORDER_LOOKUP,
                    orderId,
                    0.86,
                    "命中订单查询关键词");
        }
        if (containsAny(normalizedMessage, "适合", "课程", "怎么学", "多少钱", "政策", "faq", "知识")) {
            return new IntentRouteResult(
                    ConversationRoute.KNOWLEDGE_QA,
                    orderId,
                    0.78,
                    "命中知识问答关键词");
        }
        return new IntentRouteResult(
                ConversationRoute.DIRECT,
                orderId,
                0.40,
                "fallback：未命中业务关键词，按可直接回复问题处理");
    }

    private Optional<String> extractOrderId(String message) {
        var matcher = ORDER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    private boolean containsAny(String message, String... keywords) {
        for (var keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
