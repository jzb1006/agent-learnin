# customer-agent-app

Spring Boot 应用入口模块，后续负责：

- `/chat`
- `/health`
- 订单查询 API
- Agent 编排
- Trace 写入
- 调试台 API

Day 04 已实现基础 REST API，Day 06 已接入 Spring AI ChatClient 的最小业务边界：

| 接口 | 当前行为 |
| --- | --- |
| `GET /health` | 返回 `status=UP` 和 `service=customer-agent-app` |
| `GET /api/orders/{orderId}` | 返回内存 mock 订单；不存在时返回 `ORDER_NOT_FOUND` |
| `POST /chat` | 默认用确定性规则返回 `ORDER_LOOKUP`、`READ_ONLY`、订单证据和下一步动作；配置启用后用 Spring AI `ChatClient` 生成回复 |

当前边界：

- 不接真实数据库。
- 默认不调用真实 LLM；启用模型必须通过环境变量配置。
- 不调用 MCP Server。
- 不执行真实退款、取消或改签。

## 模型配置

默认本地测试不访问外部模型：

```yaml
spring.ai.model.chat: none
customer-agent.chat-model.enabled: false
```

启用 DeepSeek 的本地配置文件放在主工程目录 `.env`，该文件已被 `.gitignore` 忽略。应用通过 `me.paulschwarz:springboot4-dotenv` 在启动早期加载上级目录 `../.env`，并保留 Spring Boot 原生 `spring.config.import` 兜底，所以从主工程目录、`customer-agent-app` 模块目录或 IDEA 外层工程目录启动都不需要手动 `source .env`：

```bash
SPRING_AI_MODEL_CHAT=openai
CUSTOMER_AGENT_CHAT_MODEL_ENABLED=true
SPRING_AI_OPENAI_API_KEY=<your-deepseek-api-key>
SPRING_AI_OPENAI_BASE_URL=https://api.deepseek.com
SPRING_AI_OPENAI_CHAT_MODEL=deepseek-v4-flash
SPRING_AI_OPENAI_CHAT_TEMPERATURE=0.2
```

DeepSeek 走 Spring AI 的 OpenAI-compatible 通道，所以 Spring 侧仍使用 `spring.ai.openai.*` 配置名。密钥只允许通过本地 `.env`、环境变量或 Secret 注入，禁止写入仓库。

## 日志配置

应用使用 `logback-spring.xml` 输出轻量结构化日志。默认只写 stdout，适合容器和本地开发；启用 `file-log` profile 后额外写滚动文件。

每条请求日志会带上：

- `traceId`：优先透传 `X-Trace-Id`，缺失时自动生成。
- `requestId`：优先透传 `X-Request-Id`，缺失时自动生成。
- `tenantId`：优先读取 `X-Tenant-Id`，缺失时为 `-`。
- `method`、`path`、`status`、`durationMs`。

常用环境变量：

```bash
LOG_LEVEL_APP=INFO
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_SPRING_AI=INFO
LOG_LEVEL_SPRING_WEB=INFO
```

启用文件日志：

```bash
SPRING_PROFILES_ACTIVE=file-log
LOG_PATH=logs
LOG_FILE_NAME=customer-agent-app.log
LOG_FILE_MAX_SIZE=100MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE_CAP=5GB
```

业务日志只记录租户、订单号、路由、风险级别、消息长度和模型调用耗时，不记录完整用户消息或密钥。

## 验证

```bash
cd ..
mvn -pl customer-agent-app -am test -Dtest=CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false
```

模型边界定向测试：

```bash
cd ..
mvn -pl customer-agent-app -Dtest=ChatServiceModelClientTest test
```

本地启动：

```bash
cd ..
mvn -pl customer-agent-app -am package -DskipTests
java -jar customer-agent-app/target/customer-agent-app-0.1.0-SNAPSHOT.jar
```

DeepSeek 本地启动：

```bash
cd ..
mvn -pl customer-agent-app spring-boot:run
```
