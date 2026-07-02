# Day 26：Agent Loop 编排

## 目标

把 `/chat` 的执行链路收敛为可追踪的 Agent Loop：

```text
intent -> retrieve/tool -> risk check -> response -> trace
```

Day 26 只做单 Agent 编排链路的显式 trace，不引入多 Agent，不接 Prometheus / Grafana，也不改变高风险动作边界。

## 业务场景

- 开发者在 Chat Console 里发送订单、知识库、退款或人工转接问题。
- 系统不仅返回 `route / riskLevel / answer / toolCalls`，还返回本轮 Agent Loop 的步骤、证据和最终回复。
- 出现回答异常时，开发者能定位失败发生在意图识别、工具调用、风险判断、回复生成还是 trace 汇总。

## 模块边界

| 模块 | Day 26 职责 |
| --- | --- |
| `customer-agent-app` | 在 `/chat` 响应中增加 `executionTrace`，记录五步 Agent Loop |
| `customer-admin-web` | 在 Chat Console 展示 Agent Loop 步骤 |
| `customer-domain` | 不新增领域模型，继续复用 route、tool risk 和 trace 基础概念 |
| `customer-mcp-server` | 不变，仍负责 `kb_search`、`order_lookup`、`refund_policy_check` |

## 接口设计

`POST /chat` 响应新增 `executionTrace`：

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
  "toolCalls": [
    {
      "name": "order_lookup",
      "arguments": {
        "orderId": "order-1001",
        "tenantId": "tenant-demo"
      },
      "status": "SUCCEEDED",
      "riskLevel": "READ_ONLY",
      "durationMs": 3,
      "resultSummary": "order-1001 企业级 AI Agent 实战营 PAID"
    }
  ],
  "executionTrace": {
    "traceId": "trace-chat-test",
    "tenantId": "tenant-demo",
    "conversationId": "debug-session",
    "route": "ORDER_LOOKUP",
    "riskLevel": "READ_ONLY",
    "evidence": ["order:order-1001"],
    "finalAnswer": "已查询到订单 order-1001，课程为「企业级 AI Agent 实战营」，当前状态为 PAID。",
    "steps": [
      {
        "name": "intent",
        "detail": "route=ORDER_LOOKUP confidence=0.95 orderId=order-1001 reason=..."
      },
      {
        "name": "retrieve/tool",
        "detail": "order_lookup status=SUCCEEDED risk=READ_ONLY durationMs=3 summary=..."
      },
      {
        "name": "risk check",
        "detail": "riskLevel=READ_ONLY permission=java-guarded approvalRequired=false"
      },
      {
        "name": "response",
        "detail": "sources=[order:order-1001] nextActions=[展示订单状态, 等待用户继续追问] finalAnswerLength=..."
      },
      {
        "name": "trace",
        "detail": "traceId=trace-chat-test conversationId=debug-session toolCalls=1 evidence=1"
      }
    ]
  }
}
```

## 数据模型

新增应用层响应类型：

```text
CustomerAgentExecutionStep
CustomerAgentExecutionTrace
```

`CustomerAgentResponse` 新增字段：

```text
executionTrace
```

`executionTrace` 是调试和评测定位用的响应契约，不替代后续 Day 28 的 OpenTelemetry trace，也不代表持久化审计表。

## 安全边界

- `executionTrace.finalAnswer` 与 `answer` 保持一致，不额外暴露模型原始输出。
- `executionTrace.evidence` 只记录已对外返回的 `sources`。
- `retrieve/tool` 步骤只展示工具摘要，不记录完整敏感用户消息。
- 高风险退款、取消、改签仍只走政策检查和审批建议，`approvalRequired=true` 不等于真实执行。
- Prompt Injection 拦截仍发生在路由和工具调用之前。

## 验证方式

后端定向测试：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/current/bin/mvn \
  -pl customer-agent-app -am \
  -Dtest=ChatServiceModelClientTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

前端定向测试：

```bash
npm test -- --run src/App.test.tsx
```

完整收口验证：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/current/bin/mvn test
npm test -- --run
npm run build
git diff --check
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 订单查询 `/chat` | `executionTrace.traceId` 与响应 `traceId` 一致 |
| 订单查询 `/chat` | trace 记录租户、会话、route、riskLevel、证据和最终回复 |
| 订单查询 `/chat` | trace steps 顺序为 `intent -> retrieve/tool -> risk check -> response -> trace` |
| 工具调用失败 | `retrieve/tool` 仍能展示失败状态和错误摘要 |
| Web 调试台 | Chat Console 展示 Agent Loop 五步详情 |

## 原则应用

- KISS：直接在 `/chat` 响应中给出可读 trace，先满足本地调试和 Day 29 eval 定位。
- YAGNI：不提前引入多 Agent、工作流引擎、trace 存储表或观测平台。
- DRY：`executionTrace.evidence` 复用 `sources`，`finalAnswer` 复用 `answer`，工具步骤复用 `toolCalls`。
- SOLID：`ChatService` 继续负责单轮编排，新增 record 只表达响应契约，不把 UI 展示逻辑塞进服务层。
