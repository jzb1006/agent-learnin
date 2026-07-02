# customer-agent-app

Spring Boot 应用入口模块，后续负责：

- `/chat`
- `/health`
- 订单查询 API
- MCP Client 工具调用边界
- Agent 编排
- Trace 写入
- 调试台 API

Day 04 已实现基础 REST API，Day 06 已接入 Spring AI ChatClient 的最小业务边界，Day 07 已把客服 Prompt 收敛为可版本化、可测试的行为契约，Day 08 已补 `/chat` 本地意图识别 fallback，Day 09 已统一结构化客服响应，Day 15 已把工具调用结果接入 `/chat`，Day 23 已让对话编排层通过 `McpToolClient` 调用真实 stdio MCP service，Day 24 已补短期会话记忆与上下文压缩，Day 26 已补 Agent Loop execution trace：

| 接口 | 当前行为 |
| --- | --- |
| `GET /health` | 返回 `status=UP` 和 `service=customer-agent-app` |
| `GET /api/orders/{orderId}` | 返回内存 mock 订单；不存在时返回 `ORDER_NOT_FOUND` |
| `POST /chat` | 先经 `IntentRouter` 分流到 `KNOWLEDGE_QA`、`ORDER_LOOKUP`、`REFUND_OR_CANCEL`、`HUMAN_HANDOFF` 或 `DIRECT`；同租户同会话可复用最近订单上下文；通过 `McpToolClient` 调用 `kb_search`、`order_lookup`、`refund_policy_check`；统一返回 `route / answer / sources / riskLevel / nextActions / traceId / toolCalls / conversationId / memorySummary / executionTrace` |
| `kb_search(tenantId, query, topK?)` | MCP 工具名；通过 stdio 调用 `customer-mcp-server` |
| `order_lookup(orderId, tenantId)` | MCP 工具名；通过 stdio 调用 `customer-mcp-server` |
| `course_catalog(tenantId, category?)` | MCP 工具名；通过 stdio 调用 `customer-mcp-server` |

当前边界：

- 订单 API 仍使用 mock 订单仓库。
- 默认不调用真实 LLM；启用模型必须通过环境变量配置。
- `ChatService` 依赖 `McpToolClient` 抽象，不直接依赖任何 `agent.tool.*` 业务工具。
- MCP Client 默认使用 `StdioMcpToolClient` 启动并调用真实 `customer-mcp-server`。
- `customer-agent-app` 生产代码不保留本地工具实现；工具执行统一归 `customer-mcp-server`。
- `/admin/api/v1/knowledge/**` 仍是 App 侧知识库管理控制面，但 `/chat` 知识问答通过 MCP Server 的 `kb_search` 获取工具结果。
- `/chat` 的短期 Memory 使用 `tenantId + conversationId` 隔离；运行时默认写 Redis，单元测试可切换为进程内实现。
- 不执行真实退款、取消或改签。
- `handoff_to_human` 不在当前 P0 MCP Client 默认链路中暴露。

## 结构化响应

`/chat` 当前统一返回 `CustomerAgentResponse`：

```json
{
  "route": "ORDER_LOOKUP",
  "answer": "已查询到订单 order-1001，课程为「企业级 AI Agent 实战营」，当前状态为 PAID。",
  "sources": ["order:order-1001"],
  "riskLevel": "READ_ONLY",
  "nextActions": ["展示订单状态", "等待用户继续追问"],
  "traceId": "trace-chat-test",
  "conversationId": "debug-session",
  "memorySummary": "最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课",
  "executionTrace": {
    "traceId": "trace-chat-test",
    "tenantId": "tenant-demo",
    "conversationId": "debug-session",
    "route": "ORDER_LOOKUP",
    "riskLevel": "READ_ONLY",
    "evidence": ["order:order-1001"],
    "finalAnswer": "已查询到订单 order-1001，课程为「企业级 AI Agent 实战营」，当前状态为 PAID。",
    "steps": [
      {"name": "intent", "detail": "route=ORDER_LOOKUP ..."},
      {"name": "retrieve/tool", "detail": "order_lookup status=SUCCEEDED ..."},
      {"name": "risk check", "detail": "riskLevel=READ_ONLY ..."},
      {"name": "response", "detail": "sources=[order:order-1001] ..."},
      {"name": "trace", "detail": "traceId=trace-chat-test ..."}
    ]
  }
}
```

模型启用时必须返回 JSON object。服务端会解析并校验 `route`、`riskLevel`、`answer`、`sources`、`nextActions`、`traceId`，但最终 `route`、`riskLevel` 和 `traceId` 仍以 Java 层决策为准，模型不能覆盖。

## MCP Client 边界

Day 23 起，`ChatService` 只通过 `McpToolClient` 调用工具。生产运行时使用 `StdioMcpToolClient` 通过 MCP Java SDK 启动 `customer-mcp-server` JAR，并执行真实 `tools/list` / `tools/call`。

集成环境也默认启用 `stdio`：

```bash
SPRING_PROFILES_ACTIVE=integration
```

stdio 配置：

```bash
CUSTOMER_AGENT_MCP_CLIENT_MODE=stdio
CUSTOMER_AGENT_MCP_CLIENT_COMMAND=java
CUSTOMER_AGENT_MCP_CLIENT_SERVER_JAR=../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
CUSTOMER_AGENT_MCP_CLIENT_REQUEST_TIMEOUT_SECONDS=10
```

真实 stdio MCP 调用前需要先构建 server JAR：

```bash
cd ..
mvn -pl customer-mcp-server -am package
```

普通单元测试如需隔离 MCP 子进程，使用 test scope 的 fake `McpToolClient`；不要在生产代码中恢复 local 工具 adapter。

## Conversation Memory

Day 24 起，`POST /chat` 支持可选 `conversationId`：

```json
{
  "tenantId": "tenant-demo",
  "conversationId": "debug-session",
  "message": "刚才那个订单可以退款吗？"
}
```

当前 `ChatMemory` 只保存短期摘要，默认落 Redis：

- 最近有效订单号。
- route。
- 裁剪后的用户消息片段。

配置项：

```bash
CUSTOMER_AGENT_MEMORY_STORAGE=redis
CUSTOMER_AGENT_MEMORY_MAX_MESSAGE_CHARS=80
CUSTOMER_AGENT_MEMORY_MAX_SUMMARY_CHARS=320
CUSTOMER_AGENT_MEMORY_REDIS_KEY_PREFIX=customer-agent:conversation-memory
CUSTOMER_AGENT_MEMORY_TTL_SECONDS=7200
SPRING_DATA_REDIS_HOST=127.0.0.1
SPRING_DATA_REDIS_PORT=16379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_TIMEOUT=2s
```

`CUSTOMER_AGENT_MEMORY_STORAGE=in-memory` 只建议用于本地测试或临时无 Redis 调试。Redis value 是 `ChatMemorySnapshot` JSON，key 为 `<prefix>:<base64url(tenantId)>:<base64url(conversationId)>`，避免会话 ID 中的特殊字符污染 key。

上下文压缩由 `ConversationSummaryCompressor` 完成，不调用 LLM：先规范化空白，按 `max-message-chars` 裁剪单轮用户消息，再把 `最近订单 / route / 用户片段` 追加进摘要；总摘要超过 `max-summary-chars` 时保留最新尾部窗口。

该摘要只用于多轮上下文和模型提示，不允许覆盖 Java 层 route、riskLevel 或工具权限判断。

## 意图识别

当前 `IntentRouter` 是模型不可用时的确定性 fallback，负责识别五类客服入口：

| route | 风险级别 | 当前行为 |
| --- | --- | --- |
| `KNOWLEDGE_QA` | `READ_ONLY` | 只识别知识问答，等待后续 RAG 接入，不编造知识库答案 |
| `ORDER_LOOKUP` | `READ_ONLY` | 查询 mock 订单，并按配置决定是否调用模型生成回复文案 |
| `REFUND_OR_CANCEL` | `HIGH_RISK` | 只提示进入人工审批前置判断，不执行真实退款、取消或改签 |
| `HUMAN_HANDOFF` | `LOW_RISK_WRITE` | 只记录人工转接意向，不创建外部真实工单 |
| `DIRECT` | `READ_ONLY` | 未命中业务关键词时按普通客服问题处理 |

## Prompt 契约

当前 Prompt 版本：

```text
customer-agent-prompt-v1
```

Prompt 行为边界：

- 不编造订单状态、履约进度、课程时间、退款结果或未提供的业务事实。
- 不承诺真实退款、真实取消或真实改签已经成功。
- 必须基于工具、知识库或用户输入证据生成回复。
- 高风险动作必须进入审批。
- 只返回 JSON object，不输出 Markdown、代码块或额外解释。
- 系统 Prompt 不写死订单号、课程名或支付状态，具体事实只通过运行时证据传入。

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

Prompt 契约定向测试：

```bash
cd ..
mvn -pl customer-agent-app -am -Dtest=CustomerChatPromptTemplateTest -Dsurefire.failIfNoSpecifiedTests=false test
```

意图识别定向测试：

```bash
cd ..
mvn -pl customer-agent-app -am -Dtest=IntentRouterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结构化响应定向测试：

```bash
cd ..
mvn -pl customer-agent-app -Dtest=CustomerAgentResponseParserTest,ChatServiceModelClientTest test
```

Agent Loop trace 定向测试：

```bash
cd ..
mvn -pl customer-agent-app -am -Dtest=ChatServiceModelClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

MCP 职责边界定向测试：

```bash
cd ..
mvn -pl customer-agent-app -am -Dtest=McpToolOwnershipTest -Dsurefire.failIfNoSpecifiedTests=false test
```

真实 stdio MCP Client 集成测试：

```bash
cd ..
mvn -pl customer-mcp-server -am package
mvn -pl customer-agent-app -am -Dtest=StdioMcpToolClientIntegrationTest,StdioMcpToolClientApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Memory / Redis 定向测试：

```bash
cd ..
mvn -pl customer-agent-app -am -Dtest=ChatServiceModelClientTest,CustomerAgentApiTest,ConversationSummaryCompressorTest,RedisChatMemoryTest,ChatMemoryConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
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
