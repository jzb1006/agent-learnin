package com.example.customer.agent.chat;

import com.example.customer.agent.config.CustomerAgentProperties;
import com.example.customer.agent.intent.IntentRouteResult;
import com.example.customer.agent.intent.IntentRouter;
import com.example.customer.agent.observability.RequestTraceContext;
import com.example.customer.agent.order.OrderResponse;
import com.example.customer.agent.tool.OrderLookupTool;
import com.example.customer.agent.tool.RefundPolicyCheckTool;
import com.example.customer.agent.tool.RetrieveKnowledgeTool;
import com.example.customer.domain.tool.ToolResult;
import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    private final CustomerAgentProperties properties;
    private final CustomerChatModelClient chatModelClient;
    private final IntentRouter intentRouter;
    private final CustomerAgentResponseParser responseParser;
    private final OrderLookupTool orderLookupTool;
    private final RefundPolicyCheckTool refundPolicyCheckTool;
    private final RetrieveKnowledgeTool retrieveKnowledgeTool;

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
        var riskLevel = riskLevelFor(routeResult.route());
        var traceId = RequestTraceContext.currentTraceIdOr(properties.getTraceIdPrefix() + "-" + UUID.randomUUID());
        var toolExecution = executeTool(routeResult.route(), message, orderId, request.tenantId());
        var orderResponse = toolExecution.order();
        log.info(
                "chat_reply_start tenantId={} route={} orderId={} messageLength={} modelEnabled={}",
                request.tenantId(),
                routeResult.route().name(),
                orderId,
                message.length(),
                properties.getChatModel().isEnabled());
        var response = properties.getChatModel().isEnabled()
                        && routeResult.route() == ConversationRoute.ORDER_LOOKUP
                        && orderResponse != null
                ? modelResponse(request.tenantId(), message, toolExecution, routeResult.route(), riskLevel, traceId)
                : deterministicResponse(routeResult, toolExecution, riskLevel, traceId);

        log.info(
                "chat_reply_success tenantId={} orderId={} route={} riskLevel={} traceId={}",
                request.tenantId(),
                orderId,
                routeResult.route().name(),
                riskLevel.name(),
                traceId);
        return response;
    }

    private ToolExecution executeTool(ConversationRoute route, String message, String orderId, String tenantId) {
        if (route == ConversationRoute.KNOWLEDGE_QA) {
            return executeKnowledgeRetrieval(message);
        }
        if (route == ConversationRoute.ORDER_LOOKUP && orderId != null) {
            return executeOrderLookup(orderId, tenantId);
        }
        if (route == ConversationRoute.REFUND_OR_CANCEL && orderId != null) {
            return executeRefundPolicyCheck(orderId, tenantId);
        }
        return ToolExecution.empty();
    }

    private ToolExecution executeKnowledgeRetrieval(String query) {
        var knowledgeBase = properties.getKnowledgeBase();
        var tenantId = knowledgeBase.getDefaultTenantId();
        var startedAtNanos = System.nanoTime();
        var toolResult = retrieveKnowledgeTool.search(query, tenantId, knowledgeBase.getTopK());
        var durationMs = elapsedMillis(startedAtNanos);
        var toolCall = toolCall(
                RetrieveKnowledgeTool.NAME,
                Map.of("query", query, "tenantId", tenantId),
                ToolRiskLevel.READ_ONLY,
                durationMs,
                toolResult);
        return new ToolExecution(null, toolResult, List.of(toolCall));
    }

    private ToolExecution executeOrderLookup(String orderId, String tenantId) {
        var startedAtNanos = System.nanoTime();
        var toolResult = orderLookupTool.lookup(orderId, tenantId);
        var durationMs = elapsedMillis(startedAtNanos);
        var toolCall = toolCall(
                OrderLookupTool.NAME,
                Map.of("orderId", orderId, "tenantId", tenantId),
                ToolRiskLevel.READ_ONLY,
                durationMs,
                toolResult);
        return new ToolExecution(orderFrom(toolResult), toolResult, List.of(toolCall));
    }

    private ToolExecution executeRefundPolicyCheck(String orderId, String tenantId) {
        var startedAtNanos = System.nanoTime();
        var toolResult = refundPolicyCheckTool.check(orderId, tenantId);
        var durationMs = elapsedMillis(startedAtNanos);
        var toolCall = toolCall(
                RefundPolicyCheckTool.NAME,
                Map.of("orderId", orderId, "tenantId", tenantId),
                ToolRiskLevel.READ_ONLY,
                durationMs,
                toolResult);
        return new ToolExecution(orderFrom(toolResult), toolResult, List.of(toolCall));
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    private CustomerAgentToolCall toolCall(
            String name,
            Map<String, String> arguments,
            ToolRiskLevel riskLevel,
            long durationMs,
            ToolResult toolResult) {
        return new CustomerAgentToolCall(
                name,
                arguments,
                toolResult.status().name(),
                riskLevel.name(),
                durationMs,
                resultSummary(toolResult));
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
            ToolExecution toolExecution,
            ToolRiskLevel riskLevel,
            String traceId) {
        return new CustomerAgentResponse(
                routeResult.route().name(),
                deterministicAnswer(routeResult, toolExecution),
                sourcesFor(toolExecution),
                riskLevel.name(),
                nextActionsFor(routeResult.route(), toolExecution.toolResult()),
                traceId,
                toolExecution.toolCalls());
    }

    private String deterministicAnswer(IntentRouteResult routeResult, ToolExecution toolExecution) {
        var order = toolExecution.order();
        return switch (routeResult.route()) {
            case ORDER_LOOKUP -> orderLookupAnswer(order, toolExecution.toolResult());
            case KNOWLEDGE_QA -> knowledgeAnswer(toolExecution.toolResult());
            case REFUND_OR_CANCEL -> refundPolicyAnswer(toolExecution.toolResult());
            case HUMAN_HANDOFF -> "已识别到人工客服诉求。当前只记录人工转接意向，不创建外部真实工单。";
            case DIRECT -> "已收到你的问题。当前未命中订单、知识库、退款取消或人工转接意图，可先按普通客服问题处理。";
        };
    }

    private String orderLookupAnswer(OrderResponse order, ToolResult toolResult) {
        if (order == null) {
            return "未查询到可用于回复的订单证据：%s。".formatted(resultSummary(toolResult));
        }
        return "已查询到订单 " + order.id() + "，课程为「" + order.productName()
                + "」，当前状态为 " + order.status() + "。";
    }

    private String knowledgeAnswer(ToolResult toolResult) {
        if (toolResult == null || !toolResult.succeeded()) {
            return "当前知识库未检索到可引用答案，建议转人工或记录为未命中问题。";
        }
        var matches = toolMatches(toolResult);
        if (matches.isEmpty()) {
            return "当前知识库未检索到可引用答案，建议转人工或记录为未命中问题。";
        }
        var bestMatch = matches.getFirst();
        return "根据知识库「%s」：%s".formatted(
                payloadText(bestMatch, "title"),
                compactContent(payloadText(bestMatch, "content")));
    }

    private String refundPolicyAnswer(ToolResult toolResult) {
        if (toolResult == null || !toolResult.succeeded()) {
            return "已识别到退款、取消或改签诉求，但未找到可用于政策判断的订单证据，不能直接执行退款或取消操作。";
        }
        return "已完成退款政策前置检查，判断为 %s，原因：%s。该工具只返回政策建议，不执行真实退款；后续动作：%s。"
                .formatted(
                        toolPayloadText(toolResult, "policyDecision"),
                        trimTrailingSentencePunctuation(toolPayloadText(toolResult, "reason")),
                        toolPayloadText(toolResult, "recommendedAction"));
    }

    private String trimTrailingSentencePunctuation(String value) {
        return value.replaceAll("[。.!！]+$", "");
    }

    private List<String> sourcesFor(ToolExecution toolExecution) {
        if (toolExecution.order() != null) {
            return List.of("order:" + toolExecution.order().id());
        }
        var toolResult = toolExecution.toolResult();
        if (toolResult != null && toolResult.succeeded() && toolResult.payload().containsKey("orderId")) {
            return List.of("order:" + toolPayloadText(toolResult, "orderId"));
        }
        if (toolResult != null && RetrieveKnowledgeTool.NAME.equals(toolResult.toolName()) && toolResult.succeeded()) {
            return toolMatches(toolResult).stream()
                    .map(match -> payloadText(match, "source"))
                    .filter(source -> !source.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private List<String> nextActionsFor(ConversationRoute route, ToolResult toolResult) {
        return switch (route) {
            case ORDER_LOOKUP -> List.of("展示订单状态", "等待用户继续追问");
            case KNOWLEDGE_QA -> knowledgeNextActions(toolResult);
            case REFUND_OR_CANCEL -> refundNextActions(toolResult);
            case HUMAN_HANDOFF -> List.of("记录人工转接意向", "等待后续低风险写工具接入");
            case DIRECT -> List.of("直接回复用户", "必要时继续澄清问题");
        };
    }

    private List<String> knowledgeNextActions(ToolResult toolResult) {
        if (toolResult == null || !toolResult.succeeded()) {
            return List.of("记录知识库未命中", "必要时转人工");
        }
        return List.of("展示知识库来源", "等待用户继续追问");
    }

    private List<String> refundNextActions(ToolResult toolResult) {
        if (toolResult == null || !toolResult.succeeded()) {
            return List.of("进入人工审批前置判断", "禁止直接执行退款、取消或改签");
        }
        return switch (toolPayloadText(toolResult, "recommendedAction")) {
            case "CREATE_APPROVAL_REQUEST" -> List.of("创建人工审批请求", "禁止直接执行退款、取消或改签");
            case "ESCALATE_TO_HUMAN_REVIEW" -> List.of("转人工复核退款政策", "禁止直接执行退款、取消或改签");
            default -> List.of("解释退款政策", "禁止直接执行退款、取消或改签");
        };
    }

    private CustomerAgentResponse modelResponse(
            String tenantId,
            String message,
            ToolExecution toolExecution,
            ConversationRoute expectedRoute,
            ToolRiskLevel expectedRiskLevel,
            String traceId) {
        var order = toolExecution.order();
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
                    traceId,
                    toolExecution.toolCalls());
        } catch (CustomerAgentResponseParseException exception) {
            log.warn(
                    "chat_model_reply_invalid tenantId={} orderId={} reason={}",
                    tenantId,
                    order.id(),
                    exception.getMessage());
            return deterministicResponse(
                    new IntentRouteResult(expectedRoute, order.id(), 1.0, "模型结构化响应不合规，使用 Java 层确定性回复"),
                    toolExecution,
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
                order == null ? List.of() : List.of("order:" + order.id()),
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

    private OrderResponse orderFrom(ToolResult toolResult) {
        if (!toolResult.succeeded()) {
            return null;
        }
        var payload = toolResult.payload();
        if (!payload.containsKey("productName") || !payload.containsKey("status")) {
            return null;
        }
        return new OrderResponse(
                toolPayloadText(toolResult, "orderId"),
                toolPayloadText(toolResult, "tenantId"),
                toolPayloadText(toolResult, "customerId"),
                toolPayloadText(toolResult, "productName"),
                toolPayloadText(toolResult, "status"),
                Instant.parse(toolPayloadText(toolResult, "paidAt")));
    }

    private String toolPayloadText(ToolResult toolResult, String fieldName) {
        var value = toolResult.payload().get(fieldName);
        return value == null ? "" : value.toString();
    }

    private String resultSummary(ToolResult toolResult) {
        if (!toolResult.succeeded()) {
            return toolResult.errorCode().orElse("FAILED") + ": "
                    + toolResult.errorMessage().orElse("工具调用失败");
        }
        var payload = toolResult.payload();
        if (RefundPolicyCheckTool.NAME.equals(toolResult.toolName())) {
            return "%s -> %s".formatted(payload.get("policyDecision"), payload.get("recommendedAction"));
        }
        if (OrderLookupTool.NAME.equals(toolResult.toolName())) {
            return "%s %s %s".formatted(payload.get("orderId"), payload.get("productName"), payload.get("status"));
        }
        if (RetrieveKnowledgeTool.NAME.equals(toolResult.toolName())) {
            return toolMatches(toolResult).stream()
                    .findFirst()
                    .map(match -> "%s %s".formatted(payloadText(match, "title"), payloadText(match, "source")))
                    .orElse("知识库无命中");
        }
        return payload.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toolMatches(ToolResult toolResult) {
        var matches = toolResult.payload().get("matches");
        if (matches instanceof List<?> values) {
            return values.stream()
                    .filter(Map.class::isInstance)
                    .map(value -> (Map<String, Object>) value)
                    .toList();
        }
        return List.of();
    }

    private String payloadText(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        return value == null ? "" : value.toString();
    }

    private String compactContent(String content) {
        return content.replaceAll("\\s+", " ").strip();
    }

    private record ToolExecution(OrderResponse order, ToolResult toolResult, List<CustomerAgentToolCall> toolCalls) {

        private static ToolExecution empty() {
            return new ToolExecution(null, null, List.of());
        }
    }
}
