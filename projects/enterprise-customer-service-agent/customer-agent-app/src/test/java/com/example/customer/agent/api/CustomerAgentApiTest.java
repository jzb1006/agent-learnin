package com.example.customer.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.config.CustomerAgentProperties;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "customer-agent.chat-model.enabled=false",
                "spring.ai.model.chat=none"
        })
class CustomerAgentApiTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerAgentProperties properties;

    @Autowired
    private Environment environment;

    @LocalServerPort
    private int port;

    @Test
    void shouldBindCustomerAgentProperties() {
        assertThat(properties.getDefaultOrderId()).isEqualTo("order-1001");
        assertThat(properties.getTraceIdPrefix()).isEqualTo("trace");
        assertThat(properties.getChatModel().isEnabled()).isFalse();
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://api.deepseek.com");
        assertThat(environment.getProperty("spring.ai.openai.chat.model")).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void shouldDeclareLocalEnvImportForRuntimeStartup() throws IOException {
        var applicationYml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYml).contains("optional:file:.env[.properties]");
        assertThat(applicationYml).contains("optional:file:../.env[.properties]");
        assertThat(applicationYml).contains("optional:file:projects/enterprise-customer-service-agent/.env[.properties]");
    }

    @Test
    void shouldDeclareProductionReadyLogbackConfiguration() throws IOException {
        var logback = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        assertThat(logback).contains("LOG_LEVEL_APP");
        assertThat(logback).contains("traceId=%X{traceId:-}");
        assertThat(logback).contains("tenantId=%X{tenantId:-}");
        assertThat(logback).contains("requestId=%X{requestId:-}");
        assertThat(logback).contains("RollingFileAppender");
        assertThat(logback).contains("springProfile name=\"file-log\"");
    }

    @Test
    void shouldReturnApplicationHealth() throws Exception {
        var response = get("/health");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("service").asText()).isEqualTo("customer-agent-app");
    }

    @Test
    void shouldRejectUnsafeTraceHeaderAndGenerateSafeTraceId() throws Exception {
        var response = get("/health", "unsafe trace id");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id"))
                .hasValueSatisfying(traceId -> {
                    assertThat(traceId).startsWith("trace-");
                    assertThat(traceId).doesNotContain(" ");
                });
    }

    @Test
    void shouldReturnMockOrderById() throws Exception {
        var response = get("/api/orders/order-1001", "trace-api-test");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue("trace-api-test");
        assertThat(body.path("id").asText()).isEqualTo("order-1001");
        assertThat(body.path("tenantId").asText()).isEqualTo("tenant-demo");
        assertThat(body.path("customerId").asText()).isEqualTo("customer-1001");
        assertThat(body.path("productName").asText()).isEqualTo("企业级 AI Agent 实战营");
        assertThat(body.path("status").asText()).isEqualTo("PAID");
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() throws Exception {
        var response = get("/api/orders/missing-order", "trace-missing-order");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue("trace-missing-order");
        assertThat(body.path("timestamp").asText()).isNotBlank();
        assertThat(body.path("status").asInt()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(body.path("message").asText()).contains("missing-order");
        assertThat(body.path("path").asText()).isEqualTo("/api/orders/missing-order");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-missing-order");
    }

    @Test
    void shouldReturnStructuredChatResponse() throws Exception {
        var requestBody = """
                {
                  "tenantId": "tenant-demo",
                  "message": "帮我查询订单 order-1001 什么时候开课"
                }
                """;

        var response = post("/chat", requestBody, "trace-chat-test");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue("trace-chat-test");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-chat-test");
        assertThat(body.path("route").asText()).isEqualTo("ORDER_LOOKUP");
        assertThat(body.path("riskLevel").asText()).isEqualTo("READ_ONLY");
        assertThat(body.path("answer").asText()).contains("企业级 AI Agent 实战营");
        assertThat(body.path("sources").get(0).asText()).isEqualTo("order:order-1001");
        assertThat(body.path("nextActions").get(0).asText()).isEqualTo("展示订单状态");
    }

    @ParameterizedTest
    @MethodSource("stage2ChatScenarios")
    void shouldRouteStage2ChatScenariosThroughChatApi(
            String message,
            String traceId,
            String expectedRoute,
            String expectedRiskLevel,
            String expectedNextAction) throws Exception {
        var requestBody = """
                {
                  "tenantId": "tenant-demo",
                  "message": "%s"
                }
                """.formatted(message);

        var response = post("/chat", requestBody, traceId);
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue(traceId);
        assertThat(body.path("traceId").asText()).isEqualTo(traceId);
        assertThat(body.path("route").asText()).isEqualTo(expectedRoute);
        assertThat(body.path("riskLevel").asText()).isEqualTo(expectedRiskLevel);
        assertThat(body.path("answer").asText()).isNotBlank();
        assertThat(body.path("nextActions").get(0).asText()).isEqualTo(expectedNextAction);
    }

    @Test
    void shouldReturnRefundOrCancelRouteFromChatResponse() throws Exception {
        var requestBody = """
                {
                  "tenantId": "tenant-demo",
                  "message": "订单 order-1001 可以退款吗？"
                }
                """;

        var response = post("/chat", requestBody, "trace-refund-intent-test");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("traceId").asText()).isEqualTo("trace-refund-intent-test");
        assertThat(body.path("route").asText()).isEqualTo("REFUND_OR_CANCEL");
        assertThat(body.path("riskLevel").asText()).isEqualTo("HIGH_RISK");
        assertThat(body.path("answer").asText()).contains("不能直接执行退款");
        assertThat(body.path("sources").get(0).asText()).isEqualTo("order:order-1001");
        assertThat(body.path("nextActions").get(0).asText()).isEqualTo("进入人工审批前置判断");
    }

    @Test
    void shouldRejectBlankChatMessage() throws Exception {
        var requestBody = """
                {
                  "tenantId": "tenant-demo",
                  "message": " "
                }
                """;

        var response = post("/chat", requestBody, "trace-validation-test");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue("trace-validation-test");
        assertThat(body.path("status").asInt()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.path("message").asText()).contains("message");
        assertThat(body.path("path").asText()).isEqualTo("/chat");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-validation-test");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).GET().build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String traceId) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header("X-Trace-Id", traceId)
                .GET()
                .build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String traceId) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(new StringReader(body));
    }

    private static Stream<Arguments> stage2ChatScenarios() {
        return Stream.of(
                Arguments.of("新手适合学企业级 AI Agent 课程吗？", "trace-stage2-knowledge", "KNOWLEDGE_QA", "READ_ONLY", "等待 RAG 知识库接入"),
                Arguments.of("帮我查询订单 order-1001 什么时候开课", "trace-stage2-order", "ORDER_LOOKUP", "READ_ONLY", "展示订单状态"),
                Arguments.of("我要转人工客服", "trace-stage2-handoff", "HUMAN_HANDOFF", "LOW_RISK_WRITE", "记录人工转接意向"),
                Arguments.of("订单 order-1001 可以退款吗？", "trace-stage2-refund", "REFUND_OR_CANCEL", "HIGH_RISK", "进入人工审批前置判断"));
    }
}
