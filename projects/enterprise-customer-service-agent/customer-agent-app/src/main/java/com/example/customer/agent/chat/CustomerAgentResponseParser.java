package com.example.customer.agent.chat;

import com.example.customer.domain.tool.ToolRiskLevel;
import com.example.customer.domain.trace.ConversationRoute;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 客服 Agent 结构化响应解析器。
 * <p>
 * 只接受稳定 JSON 对象，所有必填字段在进入 API 响应前完成校验，避免模型自由文本污染业务契约。
 *
 * @author jiangzhibin
 * @since 2026-06-27 16:05:00
 */
@Component
public class CustomerAgentResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * 创建客服 Agent 响应解析器。
     *
     * @param objectMapper JSON 解析器
     */
    public CustomerAgentResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析模型返回的结构化 JSON。
     *
     * @param rawResponse 模型原始响应
     * @return 结构化客服 Agent 响应
     */
    public CustomerAgentResponse parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new CustomerAgentResponseParseException("模型结构化响应为空");
        }
        try {
            var root = objectMapper.readTree(rawResponse);
            if (root == null || !root.isObject()) {
                throw new CustomerAgentResponseParseException("模型结构化响应必须是 JSON object");
            }
            var route = requiredEnum(root, "route", ConversationRoute.class);
            var riskLevel = requiredEnum(root, "riskLevel", ToolRiskLevel.class);
            return new CustomerAgentResponse(
                    route,
                    requiredText(root, "answer"),
                    requiredTextArray(root, "sources"),
                    riskLevel,
                    requiredTextArray(root, "nextActions"),
                    requiredText(root, "traceId"));
        } catch (CustomerAgentResponseParseException exception) {
            throw exception;
        } catch (JacksonException exception) {
            throw new CustomerAgentResponseParseException("模型结构化响应不是合法 JSON", exception);
        }
    }

    private <E extends Enum<E>> String requiredEnum(JsonNode root, String fieldName, Class<E> enumType) {
        var value = requiredText(root, fieldName);
        try {
            return Enum.valueOf(enumType, value).name();
        } catch (IllegalArgumentException exception) {
            throw new CustomerAgentResponseParseException(fieldName + " 字段枚举值非法：" + value);
        }
    }

    private String requiredText(JsonNode root, String fieldName) {
        var value = root.get(fieldName);
        if (value == null || value.isNull() || !value.isString() || value.asString().isBlank()) {
            throw new CustomerAgentResponseParseException(fieldName + " 字段缺失或为空");
        }
        return value.asString();
    }

    private List<String> requiredTextArray(JsonNode root, String fieldName) {
        var value = root.get(fieldName);
        if (value == null || value.isNull() || !value.isArray()) {
            throw new CustomerAgentResponseParseException(fieldName + " 字段必须是字符串数组");
        }
        var result = new ArrayList<String>();
        for (var item : value) {
            if (!item.isString() || item.asString().isBlank()) {
                throw new CustomerAgentResponseParseException(fieldName + " 字段包含非法空值");
            }
            result.add(item.asString());
        }
        return List.copyOf(result);
    }
}
