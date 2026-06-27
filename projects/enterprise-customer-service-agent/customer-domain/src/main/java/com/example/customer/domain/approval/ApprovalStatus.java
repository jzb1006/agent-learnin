package com.example.customer.domain.approval;

/**
 * 审批状态。
 * <p>
 * 高风险动作创建后先进入待审批状态，由人工确认后再继续执行。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum ApprovalStatus {

    /**
     * 待审批。
     */
    PENDING,

    /**
     * 已批准。
     */
    APPROVED,

    /**
     * 已拒绝。
     */
    REJECTED
}
