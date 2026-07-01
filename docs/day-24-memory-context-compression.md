# Day 24：Memory 与上下文压缩

## 目标

让客服 Agent 支持可控的多轮会话上下文：

```text
/chat
-> tenant + conversationId
-> ChatMemory
-> 最近订单号 / 会话摘要
-> ChatService 路由和工具调用
-> 压缩摘要返回调试台
```

Day 24 的重点是在现有 `/chat` 链路里建立短期会话记忆边界：运行时默认用 Redis 保存短期摘要，本地测试仍可切换到进程内实现；同时避免把完整历史消息无限塞进模型 Prompt。

Day 24 不扩展到企业级 Memory 治理平台。结构化长期记忆、并发原子更新、记忆审计、PII 治理、压缩质量评测和多策略压缩属于 30 天主线完成后的进阶专题。

## 业务场景

典型客服对话：

1. 用户先问：“帮我查询订单 `order-1001` 什么时候开课。”
2. 系统查询订单并记录当前会话最近订单。
3. 用户继续问：“刚才那个订单可以退款吗？”
4. 系统在同租户、同会话内解析出 `order-1001`，调用 `refund_policy_check`，但仍不执行真实退款。

跨租户即使使用相同 `conversationId`，也不能共享最近订单号或摘要。

## 模块边界

| 模块 | Day 24 职责 |
| --- | --- |
| `customer-agent-app` | 增加 `ChatMemory`、`conversationId` 契约、会话摘要和上下文裁剪 |
| `customer-admin-web` | 在 Chat Console 里传入会话 ID 并展示 Memory 摘要 |
| `customer-domain` | 不新增领域模型，继续复用订单、工具和路由枚举 |
| `customer-mcp-server` | 不变，仍负责 MCP 工具执行 |

`ChatMemory` 是业务接口，当前有两个实现：

- `RedisChatMemory`：默认运行时实现，适合多实例和服务重启后的短期上下文保留。
- `InMemoryChatMemory`：本地单元测试和不启动 Redis 时的兜底实现。

两种实现都使用 `tenantId + conversationId` 作为隔离维度。Redis key 使用 URL-safe Base64 编码后的租户和会话段，避免用户传入的换行、冒号、引号污染 Redis key 空间。

## 接口设计

`POST /chat` 请求新增可选字段：

```json
{
  "tenantId": "tenant-demo",
  "conversationId": "debug-session",
  "message": "刚才那个订单可以退款吗？"
}
```

响应新增字段：

```json
{
  "conversationId": "debug-session",
  "memorySummary": "最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课"
}
```

缺少 `conversationId` 时，服务端生成一次性会话 ID。运行时租户仍以 `X-Tenant-ID` header 为准，`ChatRequest.tenantId` 只保留调试台兼容性。

## 数据模型

Day 24 不新增数据库表。短期会话状态写入 Redis，按 TTL 自动过期。

新增 Java 类型：

```text
ChatMemory
ChatMemorySnapshot(conversationId, summary, lastOrderId)
ConversationSummaryCompressor
RedisChatMemory
InMemoryChatMemory
```

配置项：

```yaml
customer-agent:
  conversation-memory:
    storage: redis
    max-message-chars: 80
    max-summary-chars: 320
    redis-key-prefix: customer-agent:conversation-memory
    ttl-seconds: 7200
```

对应环境变量：

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

摘要内容只保留：

- 最近订单号。
- route。
- 裁剪后的用户消息片段。

Redis value 是 JSON：

```json
{
  "conversationId": "debug-session",
  "summary": "最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课",
  "lastOrderId": "order-1001"
}
```

## 上下文压缩实现

当前压缩不是 LLM 总结，而是确定性窗口压缩，便于测试、审计和避免模型把敏感内容扩写进记忆。

实现步骤：

1. 规范化用户消息空白，把连续换行、制表符和多空格压成单空格。
2. 单轮用户消息先按 `max-message-chars` 裁剪，超长时保留前缀并追加 `…`。
3. 将本轮业务事实压成一行：`最近订单 <orderId>；route=<route>；用户=<裁剪后的消息>`。
4. 追加到已有摘要后，再按 `max-summary-chars` 做总窗口控制。
5. 总摘要超长时保留最新尾部，并在前面加 `…`，因为客服追问通常更依赖最近上下文。
6. Redis 只保存压缩后的 `summary` 和 `lastOrderId`，不保存完整聊天流水。

示例：

```text
第一轮：帮我查询订单 order-1001 什么时候开课
摘要：最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课

第二轮：刚才那个订单可以退款吗？
摘要：最近订单 order-1001；route=ORDER_LOOKUP；用户=... | 最近订单 order-1001；route=REFUND_OR_CANCEL；用户=刚才那个订单可以退款吗？
```

## 安全边界

- 记忆隔离键必须包含 `tenantId`，防止跨租户复用订单上下文。
- 只保存短期会话摘要，不保存密钥、token、密码、银行卡、身份证等敏感字段。
- 上下文压缩只辅助路由和回复，不允许覆盖 Java 层 route、riskLevel 和工具权限判断。
- `refund_policy_check` 仍只返回政策建议，真实退款、取消、改签继续禁止。
- Day 24 不引入长期 Memory、用户画像或跨设备同步。
- Day 24 不把企业级结构化 Memory 治理作为当天课程扩展；相关内容放到 30 天后进阶扩展。
- Redis 只是短期会话状态，依赖 TTL 自动过期，不作为审计库或长期画像库。

## 验证方式

服务层定向验证：

```bash
mvn -pl customer-agent-app -am -Dtest=ChatServiceModelClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

API 契约验证：

```bash
mvn -pl customer-agent-app -am -Dtest=CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Redis 记忆实现验证：

```bash
mvn -pl customer-agent-app -am -Dtest=ConversationSummaryCompressorTest,RedisChatMemoryTest,ChatMemoryConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

前端调试台验证：

```bash
cd customer-admin-web
npm test -- --run src/App.test.tsx
npm run build
```

完整验证：

```bash
mvn test
npm run build
git diff --check
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 同会话订单追问 | 先查询 `order-1001`，再问“刚才那个订单可以退款吗”，调用 `refund_policy_check(order-1001)` |
| 跨租户同会话 ID | `tenant-other` 不能读取 `tenant-demo` 的最近订单上下文 |
| 摘要裁剪 | 长用户消息不会完整进入下一轮模型提示，摘要长度受配置控制 |
| Redis 存储 | 使用编码后的 `tenantId + conversationId` key、JSON value 和 TTL 写入 Redis |
| Redis JSON | `conversationId` 含引号或换行时，Redis value 仍是合法 JSON |
| `/chat` JSON 契约 | 响应包含 `conversationId` 和 `memorySummary` |
| Web 调试台 | 请求体携带 `conversationId`，页面展示 Conversation ID 和 Memory 摘要 |

## 原则应用

- KISS：运行时使用 Redis 保存短期摘要，本地测试保留进程内实现，不引入额外持久化表。
- YAGNI：不做长期用户画像、跨设备同步和 LLM 总结型复杂压缩算法。
- DRY：订单上下文仍复用现有 MCP `order_lookup` / `refund_policy_check` 结果，不新增独立订单缓存模型。
- SOLID：`ChatService` 负责编排，`ChatMemory` 负责短期状态和压缩策略，MCP 工具执行仍归 `customer-mcp-server`。
