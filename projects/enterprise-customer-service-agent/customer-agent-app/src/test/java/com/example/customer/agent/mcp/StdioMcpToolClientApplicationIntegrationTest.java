package com.example.customer.agent.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.chat.ChatRequest;
import com.example.customer.agent.chat.ChatService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "customer-agent.chat-model.enabled=false",
        "customer-agent.conversation-memory.storage=in-memory",
        "customer-agent.knowledge-base.embedding-mode=local",
        "customer-agent.knowledge-base.vector-store-type=simple",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.openai.base-url="
})
@ActiveProfiles("integration")
class StdioMcpToolClientApplicationIntegrationTest {

    private static final Path MCP_SERVER_JAR = Path.of(
            "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar");

    @Autowired
    private McpToolClient mcpToolClient;

    @Autowired
    private ChatService chatService;

    @Test
    void shouldWireChatServiceToRealStdioMcpClient() {
        assertThat(Files.isRegularFile(MCP_SERVER_JAR))
                .as("先执行 mvn -pl customer-mcp-server -am package 构建真实 MCP Server JAR")
                .isTrue();

        var response = chatService.reply(new ChatRequest(
                "tenant-demo",
                "帮我查询订单 order-1001 什么时候开课"));

        assertThat(mcpToolClient).isInstanceOf(StdioMcpToolClient.class);
        assertThat(response.route()).isEqualTo("ORDER_LOOKUP");
        assertThat(response.answer()).contains("order-1001").contains("企业级 AI Agent 实战营");
        assertThat(response.sources()).containsExactly("order:order-1001");
        assertThat(response.toolCalls()).singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.name()).isEqualTo(McpToolNames.ORDER_LOOKUP);
                    assertThat(toolCall.status()).isEqualTo("SUCCEEDED");
                    assertThat(toolCall.resultSummary()).contains("PAID");
                });
    }
}
