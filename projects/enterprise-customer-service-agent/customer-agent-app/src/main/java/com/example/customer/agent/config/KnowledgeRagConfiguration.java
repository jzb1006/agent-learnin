package com.example.customer.agent.config;

import com.example.customer.agent.rag.KnowledgeDocumentLoader;
import com.example.customer.agent.rag.KnowledgeDocumentSplitter;
import com.example.customer.agent.rag.LocalKnowledgeEmbeddingModel;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 本地知识库 RAG 配置。
 * <p>
 * Day 17 使用本地 Markdown + SimpleVectorStore 跑通检索闭环；Day 18 开始支持真实 EmbeddingModel 和 pgvector。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
@Configuration
public class KnowledgeRagConfiguration {

    /**
     * 创建本地知识文档加载器。
     *
     * @param properties 客服 Agent 配置
     * @return 知识文档加载器
     */
    @Bean
    public KnowledgeDocumentLoader knowledgeDocumentLoader(CustomerAgentProperties properties) {
        return new KnowledgeDocumentLoader(Path.of(properties.getKnowledgeBase().getRootDirectory()));
    }

    /**
     * 创建知识文档切分器。
     *
     * @return 知识文档切分器
     */
    @Bean
    public KnowledgeDocumentSplitter knowledgeDocumentSplitter() {
        return new KnowledgeDocumentSplitter();
    }

    /**
     * 创建本地确定性 embedding 模型。
     *
     * @return 本地 embedding 模型
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "customer-agent.knowledge-base",
            name = "embedding-mode",
            havingValue = "local",
            matchIfMissing = true)
    public LocalKnowledgeEmbeddingModel localKnowledgeEmbeddingModel() {
        return new LocalKnowledgeEmbeddingModel();
    }

    /**
     * 创建 Spring AI 内存向量库。
     *
     * @param embeddingModel embedding 模型
     * @return 向量库
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "customer-agent.knowledge-base",
            name = "vector-store-type",
            havingValue = "simple",
            matchIfMissing = true)
    public VectorStore knowledgeVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * pgvector 模式下创建数据源。
     *
     * @param properties Spring Boot 数据源配置
     * @return PostgreSQL 数据源
     */
    @Bean
    @ConditionalOnMissingBean({DataSource.class, JdbcTemplate.class})
    @ConditionalOnProperty(prefix = "customer-agent.knowledge-base", name = "vector-store-type", havingValue = "pgvector")
    public DataSource knowledgeVectorDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * pgvector 模式下创建 JDBC 模板。
     *
     * @param dataSource PostgreSQL 数据源
     * @return JDBC 模板
     */
    @Bean
    @ConditionalOnMissingBean(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "customer-agent.knowledge-base", name = "vector-store-type", havingValue = "pgvector")
    public JdbcTemplate knowledgeVectorJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 创建 PostgreSQL pgvector 向量库。
     *
     * @param jdbcTemplate JDBC 访问模板
     * @param embeddingModel embedding 模型
     * @param properties pgvector 自动配置属性
     * @return pgvector 向量库
     */
    @Bean
    @ConditionalOnProperty(prefix = "customer-agent.knowledge-base", name = "vector-store-type", havingValue = "pgvector")
    public VectorStore pgVectorKnowledgeVectorStore(
            JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel, PgVectorStoreProperties properties) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(properties.getSchemaName())
                .idType(properties.getIdType())
                .vectorTableName(properties.getTableName())
                .vectorTableValidationsEnabled(properties.isSchemaValidation())
                .dimensions(properties.getDimensions())
                .distanceType(properties.getDistanceType())
                .removeExistingVectorStoreTable(false)
                .indexType(properties.getIndexType())
                .initializeSchema(false)
                .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
                .build();
    }
}
