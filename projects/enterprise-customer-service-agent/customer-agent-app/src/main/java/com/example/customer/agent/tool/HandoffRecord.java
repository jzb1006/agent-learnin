package com.example.customer.agent.tool;

import com.example.customer.domain.support.DomainText;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 人工转接记录。
 * <p>
 * 仅保存本地待转接事实，不代表已经调用外部工单或客服派单系统。
 *
 * @param id 转接记录标识
 * @param tenantId 租户标识
 * @param conversationId 会话标识
 * @param reason 转接原因
 * @param orderId 可选订单号
 * @param status 记录状态
 * @param createdAt 创建时间
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
public record HandoffRecord(
        String id,
        String tenantId,
        String conversationId,
        String reason,
        Optional<String> orderId,
        HandoffStatus status,
        Instant createdAt) {

    public HandoffRecord {
        id = DomainText.requireNonBlank(id, "handoff id");
        tenantId = DomainText.requireNonBlank(tenantId, "tenant id");
        conversationId = DomainText.requireNonBlank(conversationId, "conversation id");
        reason = DomainText.requireNonBlank(reason, "handoff reason");
        orderId = Objects.requireNonNull(orderId, "handoff order id must not be null")
                .map(value -> DomainText.requireNonBlank(value, "order id"));
        Objects.requireNonNull(status, "handoff status must not be null");
        Objects.requireNonNull(createdAt, "handoff created at must not be null");
    }
}
