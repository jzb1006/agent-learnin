package io.github.jiangzhibin.agentlearning.llm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleChatModelClientTest {

    private HttpServer server;
    private ExecutorService executorService;
    private CapturedRequest capturedRequest;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
        if (executorService != null) {
            executorService.close();
        }
    }

    @Test
    void completeShouldPostOpenAiCompatibleRequestAndReturnAssistantText() throws Exception {
        startServer(exchange -> {
            capturedRequest = CapturedRequest.from(exchange);
            sendJson(exchange, 200, """
                {
                  "id": "chatcmpl-test",
                  "model": "deepseek-v4-flash",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "可以先查看错误日志和最近提交。"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """);
        });

        var client = new OpenAiCompatibleChatModelClient(properties(Duration.ofSeconds(2)));

        var response = client.complete("登录接口 500，先给一个只读排查方向");

        assertEquals("可以先查看错误日志和最近提交。", response.content());
        assertEquals("deepseek-v4-flash", response.model());
        assertEquals("/chat/completions", capturedRequest.path());
        assertEquals("Bearer test-api-key", capturedRequest.authorization());
        assertTrue(capturedRequest.body().contains("\"model\":\"deepseek-v4-flash\""));
        assertTrue(capturedRequest.body().contains("\"role\":\"user\""));
        assertTrue(capturedRequest.body().contains("登录接口 500"));
        assertFalse(capturedRequest.body().contains("test-api-key"));
    }

    @Test
    void completeShouldConvertHttpErrorToSanitizedException() throws Exception {
        startServer(exchange -> sendJson(exchange, 500, """
            {
              "error": {
                "message": "upstream unavailable"
              }
            }
            """));

        var client = new OpenAiCompatibleChatModelClient(properties(Duration.ofSeconds(2)));

        var exception = assertThrows(ChatModelException.class, () -> client.complete("测试错误处理"));

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertTrue(exception.getMessage().contains("upstream unavailable"));
        assertFalse(exception.getMessage().contains("test-api-key"));
    }

    @Test
    void completeShouldConvertTimeoutToControlledException() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(500);
                sendJson(exchange, 200, """
                    {"choices":[{"message":{"content":"too late"}}]}
                    """);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IOException(interruptedException);
            }
        });

        var client = new OpenAiCompatibleChatModelClient(properties(Duration.ofMillis(50)));

        var exception = assertThrows(ChatModelException.class, () -> client.complete("测试超时"));

        assertTrue(exception.getMessage().contains("模型调用超时"));
        assertFalse(exception.getMessage().contains("test-api-key"));
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executorService);
        server.start();
    }

    private ChatModelProperties properties(Duration timeout) {
        return new ChatModelProperties(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
            "test-api-key",
            "deepseek-v4-flash",
            timeout
        );
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        var responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record CapturedRequest(String path, String authorization, String body) {

        static CapturedRequest from(HttpExchange exchange) throws IOException {
            return new CapturedRequest(
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
            );
        }
    }
}
