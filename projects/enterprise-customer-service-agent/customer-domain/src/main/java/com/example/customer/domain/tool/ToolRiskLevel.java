package com.example.customer.domain.tool;

/**
 * 工具风险级别。
 * <p>
 * Agent 只能直接执行只读或低风险工具，高风险工具必须进入人工审批边界。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public enum ToolRiskLevel {

    /**
     * 只读工具，例如知识库检索和订单查询。
     */
    READ_ONLY(false),

    /**
     * 低风险写工具，例如创建人工转接记录。
     */
    LOW_RISK_WRITE(false),

    /**
     * 高风险工具，例如真实退款、取消订单或改签。
     */
    HIGH_RISK(true);

    private final boolean requiresApproval;

    ToolRiskLevel(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    /**
     * 判断该风险级别是否需要人工审批。
     *
     * @return 需要审批返回 true
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }
}
