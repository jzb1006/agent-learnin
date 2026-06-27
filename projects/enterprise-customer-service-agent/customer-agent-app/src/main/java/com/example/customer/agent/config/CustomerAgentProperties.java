package com.example.customer.agent.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 客服 Agent 应用配置。
 * <p>
 * 集中管理本地调试阶段会被服务层复用的默认值，避免配置散落在业务代码中。
 *
 * @author jiangzhibin
 * @since 2026-06-27 10:35:00
 */
@Data
@Validated
@ConfigurationProperties(prefix = "customer-agent")
public class CustomerAgentProperties {

    /**
     * 未识别订单号时用于演示的默认订单。
     */
    @NotBlank
    private String defaultOrderId = "order-1001";

    /**
     * traceId 前缀。
     */
    @NotBlank
    private String traceIdPrefix = "trace";

    /**
     * 模型调用配置。
     */
    private ChatModel chatModel = new ChatModel();

    /**
     * 控制客服对话是否调用真实 ChatModel。
     *
     * @author jiangzhibin
     * @since 2026-06-27 10:55:00
     */
    @Data
    public static class ChatModel {

        /**
         * 是否启用真实模型调用。
         */
        private boolean enabled;
    }
}
