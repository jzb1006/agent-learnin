package com.example.customer.agent.tool;

import com.example.customer.domain.support.DomainText;
import com.example.customer.domain.tool.ToolDefinition;
import com.example.customer.domain.tool.ToolParameterSchema;
import com.example.customer.domain.tool.ToolParameterType;
import com.example.customer.domain.tool.ToolPermission;
import com.example.customer.domain.tool.ToolResult;
import com.example.customer.domain.tool.ToolRiskLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 人工转接工具。
 * <p>
 * 为 Agent 创建本地转人工记录，并明确不触发外部客服系统派单。
 *
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HandoffToHumanTool {

    public static final String NAME = "handoff_to_human";
    public static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            NAME,
            "创建本地人工转接记录，不触发外部真实派单",
            List.of(
                    ToolParameterSchema.required("tenantId", ToolParameterType.STRING, "租户 ID"),
                    ToolParameterSchema.required("conversationId", ToolParameterType.STRING, "会话 ID"),
                    ToolParameterSchema.required("reason", ToolParameterType.STRING, "转人工原因"),
                    ToolParameterSchema.optional("orderId", ToolParameterType.STRING, "关联订单号")),
            ToolRiskLevel.LOW_RISK_WRITE,
            ToolPermission.defaultFor(ToolRiskLevel.LOW_RISK_WRITE));

    private final MockHandoffRepository repository;

    /**
     * 返回人工转接工具定义。
     *
     * @return 工具定义
     */
    public ToolDefinition definition() {
        return DEFINITION;
    }

    /**
     * 创建本地人工转接记录。
     *
     * @param tenantId 租户标识
     * @param conversationId 会话标识
     * @param reason 转接原因
     * @param orderId 可选订单号
     * @return 工具结果
     */
    public ToolResult create(String tenantId, String conversationId, String reason, String orderId) {
        var startedAt = Instant.now();
        var normalizedTenantId = normalizeRequired(tenantId, "tenantId");
        var normalizedConversationId = normalizeRequired(conversationId, "conversationId");
        var normalizedReason = normalizeRequired(reason, "reason");
        if (normalizedTenantId == null || normalizedConversationId == null || normalizedReason == null) {
            return invalidArgument(firstMissingField(normalizedTenantId, normalizedConversationId, normalizedReason));
        }
        var normalizedOrderId = normalizeOptional(orderId);

        log.info(
                "tool_handoff_to_human_start tenantId={} conversationId={} orderId={}",
                normalizedTenantId,
                normalizedConversationId,
                normalizedOrderId.orElse("NONE"));
        var record = repository.create(
                normalizedTenantId,
                normalizedConversationId,
                normalizedReason,
                normalizedOrderId);
        var latency = Duration.between(startedAt, Instant.now());
        log.info(
                "tool_handoff_to_human_success handoffId={} tenantId={} conversationId={}",
                record.id(),
                record.tenantId(),
                record.conversationId());
        return ToolResult.succeeded(
                NAME,
                Map.of(
                        "handoffId", record.id(),
                        "tenantId", record.tenantId(),
                        "conversationId", record.conversationId(),
                        "orderId", record.orderId().orElse(""),
                        "status", record.status().name(),
                        "externalDispatch", false,
                        "nextAction", "WAIT_FOR_HUMAN_AGENT",
                        "trace", tracePayload(latency)));
    }

    private Map<String, Object> tracePayload(Duration latency) {
        return Map.of(
                "toolName", NAME,
                "riskLevel", ToolRiskLevel.LOW_RISK_WRITE.name(),
                "status", "SUCCEEDED",
                "latencyMs", latency.toMillis());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("tool_handoff_to_human_invalid_argument field={}", fieldName);
            return null;
        }
        return DomainText.requireNonBlank(value, fieldName);
    }

    private Optional<String> normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(DomainText.requireNonBlank(value, "orderId"));
    }

    private String firstMissingField(String tenantId, String conversationId, String reason) {
        if (tenantId == null) {
            return "tenantId";
        }
        if (conversationId == null) {
            return "conversationId";
        }
        return "reason";
    }

    private ToolResult invalidArgument(String fieldName) {
        return ToolResult.failed(NAME, ERROR_INVALID_ARGUMENT, "缺少必填参数: " + fieldName);
    }
}
