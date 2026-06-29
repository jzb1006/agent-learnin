package com.example.customer.agent.config;

import com.example.customer.agent.rag.KnowledgeDocumentLoader;
import com.example.customer.agent.rag.KnowledgeDocumentSplitter;
import com.example.customer.agent.rag.LocalKnowledgeEmbeddingModel;
import java.nio.file.Path;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地知识库 RAG 配置。
 * <p>
 * Day 17 使用本地 Markdown + SimpleVectorStore 跑通检索闭环，后续再替换为 pgvector。
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
    public LocalKnowledgeEmbeddingModel localKnowledgeEmbeddingModel() {
        return new LocalKnowledgeEmbeddingModel();
    }

    /**
     * 创建 Spring AI 本地向量库。
     *
     * @param embeddingModel 本地 embedding 模型
     * @return 向量库
     */
    @Bean
    public VectorStore knowledgeVectorStore(LocalKnowledgeEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
