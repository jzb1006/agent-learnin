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
     * MCP Client 配置。
     */
    private McpClient mcpClient = new McpClient();

    /**
     * 多轮会话记忆配置。
     */
    private ConversationMemory conversationMemory = new ConversationMemory();

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
     * 控制 Agent App 调用 MCP 工具的方式。
     *
     * @author jiangzhibin
     * @since 2026-07-01 09:56:00
     */
    @Data
    public static class McpClient {

        /**
         * MCP client 模式。
         */
        private Mode mode = Mode.STDIO;

        /**
         * stdio server 启动命令。
         */
        @NotBlank
        private String command = "java";

        /**
         * stdio server JAR 路径。
         */
        @NotBlank
        private String serverJar = "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar";

        /**
         * MCP 请求超时时间，单位秒。
         */
        private long requestTimeoutSeconds = 10;

        /**
         * MCP client 模式。
         *
         * @author jiangzhibin
         * @since 2026-07-01 09:56:00
         */
        public enum Mode {
            /**
             * 通过 stdio 启动并调用真实 customer-mcp-server。
             */
            STDIO
        }
    }

    /**
     * 控制短期会话记忆和上下文压缩策略。
     *
     * @author jiangzhibin
     * @since 2026-07-01 14:20:00
     */
    @Data
    public static class ConversationMemory {

        /**
         * 短期记忆存储类型。
         */
        private Storage storage = Storage.IN_MEMORY;

        /**
         * 单条用户消息进入摘要前的最大字符数。
         */
        private int maxMessageChars = 80;

        /**
         * 会话摘要最大字符数。
         */
        private int maxSummaryChars = 320;

        /**
         * Redis 记忆 key 前缀。
         */
        @NotBlank
        private String redisKeyPrefix = "customer-agent:conversation-memory";

        /**
         * Redis 记忆 TTL，单位秒。
         */
        private long ttlSeconds = 7200;

        /**
         * 短期记忆存储类型。
         *
         * @author jiangzhibin
         * @since 2026-07-01 16:20:00
         */
        public enum Storage {
            /**
             * 进程内存储，适合本地测试。
             */
            IN_MEMORY,

            /**
             * Redis 存储，适合生产多实例。
             */
            REDIS
        }
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
