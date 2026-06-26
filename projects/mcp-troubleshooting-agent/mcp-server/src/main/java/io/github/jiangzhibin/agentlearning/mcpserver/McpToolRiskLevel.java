package io.github.jiangzhibin.agentlearning.mcpserver;

/**
 * MCP 工具风险等级。
 * <p>
 * 当前阶段只允许低风险只读工具默认执行，高风险工具保留为权限门禁测试对象。
 *
 * @author jiangzhibin
 * @since 2026-06-24 13:49:00
 */
enum McpToolRiskLevel {

    /**
     * 低风险，只读排障查询类工具。
     */
    LOW,

    /**
     * 中风险，可能访问更大范围或更敏感上下文的工具。
     */
    MEDIUM,

    /**
     * 高风险，包含写操作、重启、部署或生产变更。
     */
    HIGH
}
