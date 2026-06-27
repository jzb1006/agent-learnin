package com.example.customer.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerAgentApiTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnApplicationHealth() throws Exception {
        var response = get("/health");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("service").asText()).isEqualTo("customer-agent-app");
    }

    @Test
    void shouldReturnMockOrderById() throws Exception {
        var response = get("/api/orders/order-1001");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("id").asText()).isEqualTo("order-1001");
        assertThat(body.path("tenantId").asText()).isEqualTo("tenant-demo");
        assertThat(body.path("customerId").asText()).isEqualTo("customer-1001");
        assertThat(body.path("productName").asText()).isEqualTo("企业级 AI Agent 实战营");
        assertThat(body.path("status").asText()).isEqualTo("PAID");
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() throws Exception {
        var response = get("/api/orders/missing-order");
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(body.path("message").asText()).contains("missing-order");
    }

    @Test
    void shouldReturnStructuredChatResponse() throws Exception {
        var requestBody = """
                {
                  "tenantId": "tenant-demo",
                  "message": "帮我查询订单 order-1001 什么时候开课"
                }
                """;

        var response = post("/chat", requestBody);
        var body = json(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("traceId").asText()).isNotBlank();
        assertThat(body.path("route").asText()).isEqualTo("ORDER_LOOKUP");
        assertThat(body.path("riskLevel").asText()).isEqualTo("READ_ONLY");
        assertThat(body.path("reply").asText()).contains("企业级 AI Agent 实战营");
        assertThat(body.path("order").path("id").asText()).isEqualTo("order-1001");
        assertThat(body.path("nextActions").get(0).asText()).isEqualTo("展示订单状态");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).GET().build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
}
