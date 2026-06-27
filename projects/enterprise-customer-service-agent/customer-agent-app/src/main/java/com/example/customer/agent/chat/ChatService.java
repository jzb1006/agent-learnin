package com.example.customer.agent.chat;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.observability.RequestTraceContext;
import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.agent.order.OrderResponse;
import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 基础客服对话服务。
 * <p>
 * 默认用确定性规则生成本地调试回复；配置启用模型后，把订单证据交给模型生成客服文案。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("order-[0-9]+");

    private final OrderLookupService orderLookupService;
    private final CustomerAgentProperties properties;
    private final CustomerChatModelClient chatModelClient;

    /**
     * 生成基础结构化客服回复。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    public ChatResponse reply(ChatRequest request) {
        var message = Optional.ofNullable(request.message()).orElse("");
        var orderId = extractOrderId(message).orElse(properties.getDefaultOrderId());
        log.info(
                "chat_reply_start tenantId={} orderId={} messageLength={} modelEnabled={}",
                request.tenantId(),
                orderId,
                message.length(),
                properties.getChatModel().isEnabled());
        var order = orderLookupService.getOrder(orderId);
        var orderResponse = OrderResponse.from(order);
        var deterministicReply = "已查询到订单 " + order.id() + "，课程为「" + order.productName()
                + "」，当前状态为 " + order.status().name() + "。";
        var reply = properties.getChatModel().isEnabled()
                ? modelReply(request.tenantId(), message, orderResponse)
                : deterministicReply;
        var traceId = RequestTraceContext.currentTraceIdOr(properties.getTraceIdPrefix() + "-" + UUID.randomUUID());

        log.info(
                "chat_reply_success tenantId={} orderId={} route={} riskLevel={} traceId={}",
                request.tenantId(),
                orderId,
                ConversationRoute.ORDER_LOOKUP.name(),
                ToolRiskLevel.READ_ONLY.name(),
                traceId);
        return new ChatResponse(
                traceId,
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

    private String modelReply(String tenantId, String message, OrderResponse order) {
        try {
            log.info("chat_model_reply_start tenantId={} orderId={}", tenantId, order.id());
            return chatModelClient.generateReply(new CustomerChatPrompt(
                    tenantId,
                    message,
                    "订单 " + order.id() + "，课程「" + order.productName() + "」，状态 " + order.status()));
        } catch (ChatModelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ChatModelException("模型调用失败", exception);
        }
    }
}
