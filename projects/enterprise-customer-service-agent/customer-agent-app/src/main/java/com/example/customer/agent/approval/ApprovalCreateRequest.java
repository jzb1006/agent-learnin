package com.example.customer.agent.approval;

import com.example.customer.domain.approval.ApprovalAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建审批请求的 API 入参。
 *
 * @param orderId 订单号
 * @param action 高风险动作
 * @param reason 审批原因
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public record ApprovalCreateRequest(
        @NotBlank(message = "orderId 不能为空") String orderId,
        @NotNull(message = "action 不能为空") ApprovalAction action,
        @NotBlank(message = "reason 不能为空") String reason) {
}
