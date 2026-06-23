package io.github.jiangzhibin.agentlearning.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolContractTest {

    @Test
    void readOnlyDefinitionShouldBeIdempotentAndExposeParameterSchema() {
        var definition = ToolDefinition.readOnly(
            "search_code",
            "按关键词搜索允许根目录内的源码",
            List.of(ToolParameter.required("keyword", ToolParameterType.STRING, "搜索关键词"))
        );

        assertEquals("search_code", definition.name());
        assertTrue(definition.readOnly());
        assertTrue(definition.idempotent());
        assertEquals("keyword", definition.parameters().getFirst().name());
        assertEquals(ToolParameterType.STRING, definition.parameters().getFirst().type());
    }

    @Test
    void definitionShouldRejectDuplicateParameterNames() {
        var parameters = List.of(
            ToolParameter.required("keyword", ToolParameterType.STRING, "搜索关键词"),
            ToolParameter.optional("keyword", ToolParameterType.STRING, "重复关键词")
        );

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolDefinition.readOnly("search_code", "搜索源码", parameters)
        );

        assertTrue(exception.getMessage().contains("keyword"));
    }

    @Test
    void toolCallShouldCopyArguments() {
        var arguments = new java.util.HashMap<String, String>();
        arguments.put("keyword", "HikariPool");

        var call = new ToolCall("search_code", arguments);
        arguments.put("keyword", "changed");

        assertEquals("HikariPool", call.arguments().get("keyword"));
        assertThrows(UnsupportedOperationException.class, () -> call.arguments().put("extra", "value"));
    }

    @Test
    void toolResultShouldSeparateSuccessAndFailureSemantics() {
        var success = ToolResult.success(
            "找到 1 个匹配片段",
            List.of(new ToolEvidence("src/main/java/App.java:12", "HikariPool timeout"))
        );
        var invalidArguments = ToolResult.invalidArguments("keyword 不能为空");

        assertEquals(ToolResultStatus.SUCCESS, success.status());
        assertEquals(1, success.evidence().size());
        assertEquals(ToolResultStatus.INVALID_ARGUMENTS, invalidArguments.status());
        assertEquals("INVALID_ARGUMENTS", invalidArguments.errorCode());
        assertEquals(List.of(), invalidArguments.evidence());
    }

    @Test
    void failedToolResultShouldRejectEvidence() {
        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ToolResult(
                ToolResultStatus.EXECUTION_FAILED,
                "读取失败",
                List.of(new ToolEvidence("src/main/java/App.java:12", "partial content")),
                "EXECUTION_FAILED",
                "读取失败"
            )
        );

        assertTrue(exception.getMessage().contains("失败工具结果不能包含证据"));
    }

    @Test
    void toolContractShouldExecuteByCallAndReturnResult() {
        TroubleshootingTool tool = new TroubleshootingTool() {
            @Override
            public ToolDefinition definition() {
                return ToolDefinition.readOnly(
                    "search_code",
                    "按关键词搜索允许根目录内的源码",
                    List.of(ToolParameter.required("keyword", ToolParameterType.STRING, "搜索关键词"))
                );
            }

            @Override
            public ToolResult execute(ToolCall call) {
                return ToolResult.success(
                    "找到匹配片段",
                    List.of(new ToolEvidence("src/main/java/App.java:12", call.arguments().get("keyword")))
                );
            }
        };

        var result = tool.execute(new ToolCall("search_code", Map.of("keyword", "HikariPool")));

        assertEquals("search_code", tool.definition().name());
        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals("HikariPool", result.evidence().getFirst().content());
    }
}
