package com.example.customer.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.customer.agent.rag.LocalKnowledgeEmbeddingModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

class KnowledgeRagConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestPropertiesConfiguration.class, KnowledgeRagConfiguration.class);

    @Test
    void shouldUseLocalEmbeddingAndSimpleVectorStoreByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LocalKnowledgeEmbeddingModel.class);
            assertThat(context).hasSingleBean(VectorStore.class);
            assertThat(context.getBean(CustomerAgentProperties.class)
                            .getKnowledgeBase()
                            .getEmbeddingMode())
                    .isEqualTo(CustomerAgentProperties.KnowledgeBase.EmbeddingMode.LOCAL);
            assertThat(context.getBean(CustomerAgentProperties.class)
                            .getKnowledgeBase()
                            .getVectorStoreType())
                    .isEqualTo(CustomerAgentProperties.KnowledgeBase.VectorStoreType.SIMPLE);
        });
    }

    @Test
    void shouldExposePgvectorCompatibleLocalEmbeddingDimensions() {
        contextRunner.run(context -> assertThat(context.getBean(LocalKnowledgeEmbeddingModel.class).dimensions())
                .isEqualTo(1536));
    }

    @Test
    void shouldUseConfiguredEmbeddingModelWhenModelModeIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        TestPropertiesConfiguration.class, ExternalEmbeddingModelConfiguration.class, KnowledgeRagConfiguration.class)
                .withPropertyValues(
                        "customer-agent.knowledge-base.embedding-mode=model",
                        "customer-agent.knowledge-base.vector-store-type=simple")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LocalKnowledgeEmbeddingModel.class);
                    var embeddingModel = context.getBean(CountingEmbeddingModel.class);
                    context.getBean(VectorStore.class).add(List.of(new Document("真实 embedding 应用于知识索引")));

                    assertThat(embeddingModel.calls()).isPositive();
                    assertThat(context.getBean(CustomerAgentProperties.class)
                                    .getKnowledgeBase()
                                    .getEmbeddingMode())
                            .isEqualTo(CustomerAgentProperties.KnowledgeBase.EmbeddingMode.MODEL);
                });
    }

    @Test
    void shouldCreatePgVectorStoreWhenPgvectorModeIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        TestPropertiesConfiguration.class,
                        ExternalEmbeddingModelConfiguration.class,
                        JdbcTemplateConfiguration.class,
                        KnowledgeRagConfiguration.class)
                .withPropertyValues(
                        "customer-agent.knowledge-base.embedding-mode=model",
                        "customer-agent.knowledge-base.vector-store-type=pgvector")
                .run(context -> {
                    assertThat(context.getBean(VectorStore.class)).isInstanceOf(PgVectorStore.class);
                    assertThat(context.getBean(RecordingJdbcTemplate.class).executions()).isZero();
                });
    }

    @Configuration
    @EnableConfigurationProperties({CustomerAgentProperties.class, DataSourceProperties.class, PgVectorStoreProperties.class})
    static class TestPropertiesConfiguration {
    }

    @Configuration
    static class ExternalEmbeddingModelConfiguration {

        @Bean
        CountingEmbeddingModel countingEmbeddingModel() {
            return new CountingEmbeddingModel();
        }
    }

    @Configuration
    static class JdbcTemplateConfiguration {

        @Bean
        RecordingJdbcTemplate jdbcTemplate() {
            return new RecordingJdbcTemplate();
        }
    }

    static class RecordingJdbcTemplate extends JdbcTemplate {

        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public void afterPropertiesSet() {
            // 测试只验证 PgVectorStore 装配是否会执行 DDL，不需要真实 DataSource。
        }

        @Override
        public void execute(String sql) {
            executions.incrementAndGet();
        }

        int executions() {
            return executions.get();
        }
    }

    static class CountingEmbeddingModel implements EmbeddingModel {

        private static final int DIMENSIONS = 8;

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            var embeddings = new ArrayList<Embedding>();
            var index = 0;
            for (var instruction : request.getInstructions()) {
                embeddings.add(new Embedding(vectorize(instruction), index++));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vectorize(getEmbeddingContent(document));
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }

        int calls() {
            return calls.get();
        }

        private float[] vectorize(String value) {
            calls.incrementAndGet();
            var vector = new float[DIMENSIONS];
            vector[Math.floorMod(value == null ? 0 : value.hashCode(), vector.length)] = 1.0f;
            return vector;
        }
    }
}
