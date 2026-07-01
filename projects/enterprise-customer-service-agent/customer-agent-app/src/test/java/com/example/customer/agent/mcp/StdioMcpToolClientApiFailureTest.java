package com.example.customer.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * stdio MCP 客户端 API 失败路径测试。
 * <p>
 * 验证 MCP transport 初始化失败时，`/chat` 仍返回可调试的结构化 Agent 响应，而不是 servlet 500。
 *
 * @author jiangzhibin
 * @since 2026-07-01 11:28:00
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "customer-agent.chat-model.enabled=false",
                "customer-agent.knowledge-base.embedding-mode=local",
                "customer-agent.knowledge-base.vector-store-type=simple",
                "customer-agent.mcp-client.mode=stdio",
                "customer-agent.mcp-client.command=definitely-not-a-mcp-command",
                "customer-agent.mcp-client.server-jar=unused.jar",
                "customer-agent.mcp-client.request-timeout-seconds=1",
                "spring.ai.model.chat=none",
                "spring.ai.model.embedding=none",
                "spring.ai.openai.base-url="
        })
class StdioMcpToolClientApiFailureTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnStructuredChatResponseWhenStdioMcpClientCannotStart() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/chat"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("X-Tenant-ID", "tenant-demo")
                .header("X-Trace-Id", "trace-mcp-failure-chat")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "tenantId": "tenant-demo",
                          "message": "帮我查询订单 order-1001 什么时候开课"
                        }
                        """))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("traceId").asText()).isEqualTo("trace-mcp-failure-chat");
        assertThat(body.path("route").asText()).isEqualTo("ORDER_LOOKUP");
        assertThat(body.path("answer").asText()).contains("未查询到可用于回复的订单证据");
        assertThat(body.path("toolCalls").get(0).path("name").asText()).isEqualTo(McpToolNames.ORDER_LOOKUP);
        assertThat(body.path("toolCalls").get(0).path("status").asText()).isEqualTo("FAILED");
        assertThat(body.path("toolCalls").get(0).path("resultSummary").asText())
                .contains("MCP_CLIENT_CALL_FAILED");
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(new StringReader(body));
    }
}
