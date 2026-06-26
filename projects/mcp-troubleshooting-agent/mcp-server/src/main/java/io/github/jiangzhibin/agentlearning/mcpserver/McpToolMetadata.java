package io.github.jiangzhibin.agentlearning.mcpserver;

import java.util.Map;

/**
 * MCP 工具权限元数据。
 * <p>
 * 该元数据用于让 Agent 在工具发现阶段理解工具风险，并让 Server 在执行前做默认权限门禁。
 *
 * @author jiangzhibin
 * @since 2026-06-24 13:49:00
 */
record McpToolMetadata(
    McpToolRiskLevel riskLevel,
    boolean readOnly
) {

    McpToolMetadata {
        if (riskLevel == null) {
            throw new IllegalArgumentException("工具风险等级不能为空");
        }
    }

    /**
     * 创建低风险只读工具元数据。
     *
     * @return 低风险只读工具元数据
     */
    static McpToolMetadata readOnlyLowRisk() {
        return new McpToolMetadata(McpToolRiskLevel.LOW, true);
    }

    /**
     * 创建高风险写操作工具元数据。
     *
     * @return 高风险写操作工具元数据
     */
    static McpToolMetadata highRiskWrite() {
        return new McpToolMetadata(McpToolRiskLevel.HIGH, false);
    }

    /**
     * 判断工具是否允许默认执行。
     *
     * @return 仅低风险只读工具允许默认执行
     */
    boolean defaultAllowed() {
        return readOnly && McpToolRiskLevel.LOW == riskLevel;
    }

    /**
     * 转为 MCP Tool meta 字段。
     *
     * @return 可序列化的工具元数据
     */
    Map<String, Object> toMeta() {
        return Map.of(
            "riskLevel", riskLevel.name(),
            "readOnly", readOnly
        );
    }
}
