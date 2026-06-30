# deploy

部署与运行配置目录。

后续用于 Docker Compose、SQL 脚本、Prometheus、Grafana 和远程部署说明。远程执行前必须再次确认影响范围。

## Day 18 本地 pgvector

本地 PostgreSQL / pgvector 只用于开发验证：

```bash
docker compose -f deploy/docker-compose.pgvector.yml up -d
```

初始化 SQL：

```text
deploy/sql/001_init_pgvector.sql
```

该脚本创建 `vector`、`hstore`、`uuid-ossp` 扩展、`public.customer_knowledge_vectors` 表和 HNSW 余弦索引。

应用默认仍使用本地 fallback：

```properties
SPRING_AI_MODEL_EMBEDDING=none
CUSTOMER_AGENT_KNOWLEDGE_EMBEDDING_MODE=local
CUSTOMER_AGENT_KNOWLEDGE_VECTOR_STORE_TYPE=simple
```

启用真实 EmbeddingModel + pgvector 时，需要先启动本地 PostgreSQL，并在本机 `.env` 中切换：

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

Chat 模型和 Embedding 模型必须分开配置：DeepSeek 可继续作为 Chat provider；Embedding 使用百炼 / DashScope 的 OpenAI-compatible `text-embedding-v4`。如果百炼控制台提供业务空间专属域名，优先把 `CUSTOMER_AGENT_EMBEDDING_OPENAI_BASE_URL` 改为该专属域名。

`customer-agent-app` 显式关闭 pgvector 自动建表；远程数据库执行 SQL 前必须再次确认目标库、脚本、影响表和验证方式。
