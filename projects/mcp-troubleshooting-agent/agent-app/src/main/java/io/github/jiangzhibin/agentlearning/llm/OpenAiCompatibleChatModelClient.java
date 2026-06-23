package io.github.jiangzhibin.agentlearning.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible Chat API 客户端。
 * <p>
 * 当前用于 DeepSeek 非流式文本调用，后续结构化输出和工具调用在此基础上扩展请求体。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:14:15
 */
@RequiredArgsConstructor
public class OpenAiCompatibleChatModelClient implements ChatModelClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatModelProperties properties;
    private final HttpClient httpClient;

    /**
     * 使用默认 JDK HttpClient 创建模型客户端。
     *
     * @param properties 模型调用配置
     */
    public OpenAiCompatibleChatModelClient(ChatModelProperties properties) {
        this(
            properties,
            HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build()
        );
    }

    @Override
    public ChatModelResponse complete(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("用户问题不能为空");
        }

        var request = buildRequest(userQuestion);

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (HttpTimeoutException timeoutException) {
            throw new ChatModelException(
                "模型调用超时，timeout=" + properties.timeout().toMillis() + "ms",
                timeoutException
            );
        } catch (IOException ioException) {
            throw new ChatModelException(sanitize("模型调用失败：" + ioException.getMessage()), ioException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ChatModelException("模型调用被中断", interruptedException);
        }
    }

    private HttpRequest buildRequest(String userQuestion) {
        try {
            var payload = new ChatCompletionRequest(
                properties.modelName(),
                List.of(new ChatMessage("user", userQuestion)),
                false
            );
            var requestBody = OBJECT_MAPPER.writeValueAsString(payload);

            return HttpRequest.newBuilder(chatCompletionUri())
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        } catch (IOException ioException) {
            throw new ChatModelException("模型请求构建失败", ioException);
        }
    }

    private URI chatCompletionUri() {
        var baseUrl = properties.baseUrl().toString();
        var normalizedBaseUrl = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
        return URI.create(normalizedBaseUrl + "/chat/completions");
    }

    private ChatModelResponse parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ChatModelException(sanitize(
                "模型调用返回 HTTP " + response.statusCode() + "：" + response.body()
            ));
        }

        try {
            var completion = OBJECT_MAPPER.readValue(response.body(), ChatCompletionResponse.class);
            if (completion.choices() == null || completion.choices().isEmpty()) {
                throw new ChatModelException("模型响应缺少 choices");
            }

            var firstChoice = completion.choices().getFirst();
            if (firstChoice.message() == null || firstChoice.message().content() == null
                || firstChoice.message().content().isBlank()) {
                throw new ChatModelException("模型响应缺少助手文本");
            }

            var model = completion.model() == null || completion.model().isBlank()
                ? properties.modelName()
                : completion.model();
            return new ChatModelResponse(firstChoice.message().content(), model);
        } catch (IOException ioException) {
            throw new ChatModelException("模型响应解析失败", ioException);
        }
    }

    private String sanitize(String message) {
        return message.replace(properties.apiKey(), "[REDACTED]");
    }

    private record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        boolean stream
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(String model, List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(AssistantMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssistantMessage(String content) {
    }
}
