package io.github.jiangzhibin.agentlearning.mcpserver;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 带默认权限门禁的 MCP 工具规格构造器。
 * <p>
 * 该构造器负责把业务权限元数据写入 MCP Tool meta，并在 handler 执行前拒绝非默认允许的工具。
 *
 * @author jiangzhibin
 * @since 2026-06-24 13:49:00
 */
final class GuardedMcpToolSpecification {

    private GuardedMcpToolSpecification() {
    }

    /**
     * 创建带权限元数据和执行门禁的工具规格。
     *
     * @param delegate 原始 MCP 工具规格
     * @param metadata 工具权限元数据
     * @return 带权限门禁的 MCP 工具规格
     */
    static McpServerFeatures.SyncToolSpecification create(
        McpServerFeatures.SyncToolSpecification delegate,
        McpToolMetadata metadata
    ) {
        if (delegate == null) {
            throw new IllegalArgumentException("原始工具规格不能为空");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("工具权限元数据不能为空");
        }
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(withMetadata(delegate.tool(), metadata))
            .callHandler((exchange, request) -> {
                if (!metadata.defaultAllowed()) {
                    return McpToolResults.failure(
                        McpToolResults.PERMISSION_DENIED,
                        "高风险工具或非只读工具默认不执行：" + request.name()
                    );
                }
                return delegate.callHandler().apply(exchange, request);
            })
            .build();
    }

    private static McpSchema.Tool withMetadata(McpSchema.Tool tool, McpToolMetadata metadata) {
        if (tool == null) {
            throw new IllegalArgumentException("MCP 工具定义不能为空");
        }
        var mergedMeta = new LinkedHashMap<String, Object>();
        if (tool.meta() != null) {
            mergedMeta.putAll(tool.meta());
        }
        mergedMeta.putAll(metadata.toMeta());
        return new McpSchema.Tool(
            tool.name(),
            tool.title(),
            tool.description(),
            tool.inputSchema(),
            tool.outputSchema(),
            tool.annotations(),
            Map.copyOf(mergedMeta),
            tool.icons()
        );
    }
}
