package com.example.customer.agent.chat;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.intent.IntentRouteResult;
import com.example.customer.agent.intent.IntentRouter;
import com.example.customer.agent.observability.RequestTraceContext;
import com.example.customer.agent.order.OrderLookupService;
import com.example.customer.agent.order.OrderResponse;
import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.List;
import java.util.UUID;
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

    private final OrderLookupService orderLookupService;
    private final CustomerAgentProperties properties;
    private final CustomerChatModelClient chatModelClient;
    private final IntentRouter intentRouter;
    private final CustomerAgentResponseParser responseParser;

    /**
     * 生成基础结构化客服回复。
     *
     * @param request 对话请求
     * @return 客服 Agent 结构化响应
     */
    public CustomerAgentResponse reply(ChatRequest request) {
        var message = request.message() == null ? "" : request.message();
        var routeResult = intentRouter.route(message);
        var orderId = orderIdFor(routeResult);
        var orderResponse = orderId == null ? null : OrderResponse.from(orderLookupService.getOrder(orderId));
        var riskLevel = riskLevelFor(routeResult.route());
        var traceId = RequestTraceContext.currentTraceIdOr(properties.getTraceIdPrefix() + "-" + UUID.randomUUID());
        log.info(
                "chat_reply_start tenantId={} route={} orderId={} messageLength={} modelEnabled={}",
                request.tenantId(),
                routeResult.route().name(),
                orderId,
                message.length(),
                properties.getChatModel().isEnabled());
        var response = properties.getChatModel().isEnabled() && routeResult.route() == ConversationRoute.ORDER_LOOKUP
                ? modelResponse(request.tenantId(), message, orderResponse, routeResult.route(), riskLevel, traceId)
                : deterministicResponse(routeResult, orderResponse, riskLevel, traceId);

        log.info(
                "chat_reply_success tenantId={} orderId={} route={} riskLevel={} traceId={}",
                request.tenantId(),
                orderId,
                routeResult.route().name(),
                riskLevel.name(),
                traceId);
        return response;
    }

    private String orderIdFor(IntentRouteResult routeResult) {
        if (routeResult.route() == ConversationRoute.ORDER_LOOKUP) {
            return routeResult.orderId() == null ? properties.getDefaultOrderId() : routeResult.orderId();
        }
        if (routeResult.route() == ConversationRoute.REFUND_OR_CANCEL) {
            return routeResult.orderId();
        }
        return null;
    }

    private ToolRiskLevel riskLevelFor(ConversationRoute route) {
        return switch (route) {
            case KNOWLEDGE_QA, ORDER_LOOKUP, DIRECT -> ToolRiskLevel.READ_ONLY;
            case REFUND_OR_CANCEL -> ToolRiskLevel.HIGH_RISK;
            case HUMAN_HANDOFF -> ToolRiskLevel.LOW_RISK_WRITE;
        };
    }

    private CustomerAgentResponse deterministicResponse(
            IntentRouteResult routeResult,
            OrderResponse order,
            ToolRiskLevel riskLevel,
            String traceId) {
        return new CustomerAgentResponse(
                routeResult.route().name(),
                deterministicAnswer(routeResult, order),
                sourcesFor(order),
                riskLevel.name(),
                nextActionsFor(routeResult.route()),
                traceId);
    }

    private String deterministicAnswer(IntentRouteResult routeResult, OrderResponse order) {
        return switch (routeResult.route()) {
            case ORDER_LOOKUP -> "已查询到订单 " + order.id() + "，课程为「" + order.productName()
                    + "」，当前状态为 " + order.status() + "。";
            case KNOWLEDGE_QA -> "已识别为知识库问答问题。当前 Day 08 只完成意图识别，知识库回答将在 RAG 接入后提供。";
            case REFUND_OR_CANCEL -> "已识别到退款、取消或改签诉求，不能直接执行退款或取消操作，后续必须进入人工审批前置判断。";
            case HUMAN_HANDOFF -> "已识别到人工客服诉求。当前只记录人工转接意向，不创建外部真实工单。";
            case DIRECT -> "已收到你的问题。当前未命中订单、知识库、退款取消或人工转接意图，可先按普通客服问题处理。";
        };
    }

    private List<String> sourcesFor(OrderResponse order) {
        return order == null ? List.of() : List.of("order:" + order.id());
    }

    private List<String> nextActionsFor(ConversationRoute route) {
        return switch (route) {
            case ORDER_LOOKUP -> List.of("展示订单状态", "等待用户继续追问");
            case KNOWLEDGE_QA -> List.of("等待 RAG 知识库接入", "保留用户问题用于后续知识库验证");
            case REFUND_OR_CANCEL -> List.of("进入人工审批前置判断", "禁止直接执行退款、取消或改签");
            case HUMAN_HANDOFF -> List.of("记录人工转接意向", "等待后续低风险写工具接入");
            case DIRECT -> List.of("直接回复用户", "必要时继续澄清问题");
        };
    }

    private CustomerAgentResponse modelResponse(
            String tenantId,
            String message,
            OrderResponse order,
            ConversationRoute expectedRoute,
            ToolRiskLevel expectedRiskLevel,
            String traceId) {
        try {
            log.info("chat_model_reply_start tenantId={} orderId={}", tenantId, order.id());
            var rawResponse = chatModelClient.generateReply(new CustomerChatPrompt(
                    tenantId,
                    message,
                    modelEvidence(order, expectedRoute, expectedRiskLevel, traceId)));
            var parsed = responseParser.parse(rawResponse);
            requireModelMetadataMatchesJavaDecision(parsed, expectedRoute, expectedRiskLevel);
            return new CustomerAgentResponse(
                    expectedRoute.name(),
                    parsed.answer(),
                    parsed.sources(),
                    expectedRiskLevel.name(),
                    parsed.nextActions(),
                    traceId);
        } catch (CustomerAgentResponseParseException exception) {
            log.warn(
                    "chat_model_reply_invalid tenantId={} orderId={} reason={}",
                    tenantId,
                    order.id(),
                    exception.getMessage());
            return deterministicResponse(
                    new IntentRouteResult(expectedRoute, order.id(), 1.0, "模型结构化响应不合规，使用 Java 层确定性回复"),
                    order,
                    expectedRiskLevel,
                    traceId);
        } catch (ChatModelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ChatModelException("模型调用失败", exception);
        }
    }

    private String modelEvidence(
            OrderResponse order,
            ConversationRoute expectedRoute,
            ToolRiskLevel expectedRiskLevel,
            String traceId) {
        return """
                route=%s
                riskLevel=%s
                sources=%s
                traceId=%s
                订单 %s，课程「%s」，状态 %s
                """.formatted(
                expectedRoute.name(),
                expectedRiskLevel.name(),
                sourcesFor(order),
                traceId,
                order.id(),
                order.productName(),
                order.status()).strip();
    }

    private void requireModelMetadataMatchesJavaDecision(
            CustomerAgentResponse parsed,
            ConversationRoute expectedRoute,
            ToolRiskLevel expectedRiskLevel) {
        if (!expectedRoute.name().equals(parsed.route())) {
            throw new CustomerAgentResponseParseException(
                    "route 字段与 Java 路由不一致：expected=" + expectedRoute.name() + ", actual=" + parsed.route());
        }
        if (!expectedRiskLevel.name().equals(parsed.riskLevel())) {
            throw new CustomerAgentResponseParseException(
                    "riskLevel 字段与 Java 风险级别不一致：expected="
                            + expectedRiskLevel.name()
                            + ", actual="
                            + parsed.riskLevel());
        }
    }
}
