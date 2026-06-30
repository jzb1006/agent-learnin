# Day 18：引入 pgvector

## 目标

把 Day 17 的本地确定性 embedding 和内存向量库，升级为可切换的真实 `EmbeddingModel` 与 PostgreSQL / pgvector 基础设施。

Day 18 不默认连接远程服务器，不执行远程 DDL。数据库结构通过 SQL 脚本入仓库，后续人工确认后再执行。

## 业务场景

用户询问 FAQ、退款政策、课程产品时，客服 Agent 需要基于知识库语义检索返回来源。Day 17 的 `LocalKnowledgeEmbeddingModel` 可以验证流程，但不能代表真实语义质量；Day 18 增加生产化向量库底座，为后续多租户知识库和知识管理 API 做准备。

## 模块边界

| 模块 | 职责 |
| --- | --- |
| `customer-agent-app` | 配置 `EmbeddingModel` 和 `VectorStore`，启动时重建本地知识索引 |
| `knowledge-base` | 保存 Markdown 知识文档和元数据 |
| `deploy` | 保存本地 pgvector Compose 与初始化 SQL |

## 实现要点

- 默认仍使用 `embedding-mode=local` 和 `vector-store-type=simple`，保证本地测试不依赖外部模型或数据库。
- 启用真实模型时，设置 `SPRING_AI_MODEL_EMBEDDING=openai` 和 `CUSTOMER_AGENT_KNOWLEDGE_EMBEDDING_MODE=model`。
- 启用 pgvector 时，设置 `CUSTOMER_AGENT_KNOWLEDGE_VECTOR_STORE_TYPE=pgvector`。
- `customer-agent-app` 排除 Spring AI 的 pgvector 自动配置，并显式使用 `initializeSchema(false)`，避免应用启动时自动执行 DDL。

## 配置

本地 pgvector：

```bash
docker compose -f deploy/docker-compose.pgvector.yml up -d
```

真实 embedding + pgvector：

```properties
SPRING_AI_MODEL_EMBEDDING=openai
CUSTOMER_AGENT_KNOWLEDGE_EMBEDDING_MODE=model
CUSTOMER_AGENT_KNOWLEDGE_VECTOR_STORE_TYPE=pgvector
CUSTOMER_AGENT_EMBEDDING_OPENAI_API_KEY=<your-dashscope-api-key>
CUSTOMER_AGENT_EMBEDDING_OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
CUSTOMER_AGENT_EMBEDDING_OPENAI_MODEL=text-embedding-v4
CUSTOMER_AGENT_EMBEDDING_DIMENSIONS=1536
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:15432/customer_agent
SPRING_DATASOURCE_USERNAME=customer_agent
SPRING_DATASOURCE_PASSWORD=customer_agent_dev
```

Chat 模型和 Embedding 模型必须分开配置。DeepSeek 可继续用于 Chat；Embedding 使用百炼 / DashScope 的 OpenAI-compatible `text-embedding-v4`。如果百炼控制台提供业务空间专属域名，优先使用专属域名：

```properties
CUSTOMER_AGENT_EMBEDDING_OPENAI_BASE_URL=https://<your-workspace-id>.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
```

## 数据模型

初始化脚本：

```text
projects/enterprise-customer-service-agent/deploy/sql/001_init_pgvector.sql
```

核心表：

| 表 | 说明 |
| --- | --- |
| `public.customer_knowledge_vectors` | Spring AI pgvector 向量表，保存知识 chunk、metadata 和 1536 维 embedding |

索引：

| 索引 | 说明 |
| --- | --- |
| `customer_knowledge_vectors_embedding_idx` | HNSW + cosine，用于语义检索 |

## 安全边界

- `.env` 不入仓库，`.env.example` 只放占位或本地开发默认值。
- 远程数据库不由应用自动建表。
- 远程执行 `001_init_pgvector.sql` 前必须确认目标库、影响表和回滚或补救方式。

## 验证方式

已验证：

```bash
mvn -pl customer-agent-app -am test -Dtest=KnowledgeRagConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```

测试覆盖：

- 默认模式创建 `LocalKnowledgeEmbeddingModel` 与 `SimpleVectorStore`。
- `embedding-mode=model` 时使用外部 `EmbeddingModel`。
- `vector-store-type=pgvector` 时创建 `PgVectorStore`，且不会执行 `JdbcTemplate.execute(...)` 自动建表。

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 默认配置启动 | 不需要模型 API Key，不需要 PostgreSQL |
| `embedding-mode=model` | 使用 Spring AI 自动配置的真实 `EmbeddingModel` |
| `vector-store-type=pgvector` | 装配 `PgVectorStore`，DDL 由 SQL 脚本负责 |
