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
     * 知识库配置。
     */
    private KnowledgeBase knowledgeBase = new KnowledgeBase();

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

    /**
     * 控制本地知识库加载与检索。
     *
     * @author jiangzhibin
     * @since 2026-06-29 20:15:00
     */
    @Data
    public static class KnowledgeBase {

        /**
         * Embedding 模型模式。
         */
        private EmbeddingMode embeddingMode = EmbeddingMode.LOCAL;

        /**
         * 向量库类型。
         */
        private VectorStoreType vectorStoreType = VectorStoreType.SIMPLE;

        /**
         * 本地 Markdown 知识库根目录。
         */
        @NotBlank
        private String rootDirectory = "../knowledge-base";

        /**
         * 本地默认知识租户。
         */
        @NotBlank
        private String defaultTenantId = "default";

        /**
         * 默认召回条数。
         */
        private int topK = 3;

        /**
         * 知识库 embedding 来源。
         *
         * @author jiangzhibin
         * @since 2026-06-29 18:15:00
         */
        public enum EmbeddingMode {
            /**
             * 使用本地确定性 fallback embedding。
             */
            LOCAL,

            /**
             * 使用 Spring AI 自动配置的真实 EmbeddingModel。
             */
            MODEL
        }

        /**
         * 知识库向量存储类型。
         *
         * @author jiangzhibin
         * @since 2026-06-29 18:15:00
         */
        public enum VectorStoreType {
            /**
             * 使用内存 SimpleVectorStore。
             */
            SIMPLE,

            /**
             * 使用 PostgreSQL pgvector。
             */
            PGVECTOR
        }
    }
}
