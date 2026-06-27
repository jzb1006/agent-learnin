package com.example.customer.agent.chat;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.agent.order.OrderResponse;
import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 基础客服对话服务。
 * <p>
 * Day 04 不调用大模型，只用确定性规则把订单查询问题映射为结构化响应。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("order-[0-9]+");

    private final OrderLookupService orderLookupService;
    private final CustomerAgentProperties properties;

    /**
     * 生成基础结构化客服回复。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    public ChatResponse reply(ChatRequest request) {
        var message = Optional.ofNullable(request.message()).orElse("");
        var orderId = extractOrderId(message).orElse(properties.getDefaultOrderId());
        var order = orderLookupService.getOrder(orderId);
        var orderResponse = OrderResponse.from(order);
        var reply = "已查询到订单 " + order.id() + "，课程为「" + order.productName()
                + "」，当前状态为 " + order.status().name() + "。";

        return new ChatResponse(
                properties.getTraceIdPrefix() + "-" + UUID.randomUUID(),
                ConversationRoute.ORDER_LOOKUP.name(),
                ToolRiskLevel.READ_ONLY.name(),
                reply,
                orderResponse,
                List.of("展示订单状态", "等待用户继续追问"));
    }

    private Optional<String> extractOrderId(String message) {
        var matcher = ORDER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }
}
