# Day 21：设计 MCP 边界

## 目标

为客服订单 Agent 明确 MCP 暴露边界：

```text
Agent 编排只依赖 MCP 能力契约，具体工具执行收敛到 MCP Server 侧。
```

Day 21 只做 tools / resources / prompts 的边界设计，不实现 MCP Server，不接 MCP Client，不改变当前 `/chat` 的直连工具调用路径。

## 业务场景

客服 Agent 后续需要把订单查询、知识库检索、课程目录和退款政策检查暴露给 MCP Host / Client。暴露边界必须满足：

- 多租户请求不能跨租户读取订单或知识。
- MCP 工具默认只暴露只读能力。
- 退款、取消、改签等高风险动作不能通过 MCP 绕过审批。
- Agent App 后续可以通过 MCP Client 调用工具，而不是直接依赖工具实现类。

## 模块边界

| 模块 | Day 21 定位 |
| --- | --- |
| `customer-domain` | 保持 framework-free 的工具契约层，继续承载 `ToolDefinition`、`ToolPermission`、`ToolRiskLevel`、`ToolResult` |
| `customer-agent-app` | 当前仍保留本地工具实现和 `/chat` 调用路径；Day 23 再迁移为 MCP Client 调用 |
| `customer-mcp-server` | MCP 能力暴露模块；Day 22 实现 MCP Server 时负责注册 tools/resources/prompts |
| `knowledge-base` | 知识源和 RAG 数据来源，不直接作为 MCP 文件系统根暴露 |
| `customer-admin-web` | 继续作为调试台；不直接承担 MCP Inspector 或完整 MCP 管理台职责 |

依赖方向约束：

```text
customer-domain <- customer-mcp-server
customer-domain <- customer-agent-app
```

`customer-agent-app` 和 `customer-mcp-server` 不应互相依赖具体 Controller 或 Spring Boot 启动类。Day 22 如需复用当前工具实现，应优先抽出共享工具用例或 adapter，而不是复制业务逻辑，也不要让 MCP Server 直接调用 Web Controller。

## MCP Tools 设计

Day 22 的 P0 MCP tools 只暴露只读能力。

| Tool | 风险级别 | 默认策略 | 参数 | 返回 |
| --- | --- | --- | --- | --- |
| `kb_search` | `READ_ONLY` | 默认允许 | `tenantId`、`query`、`topK?` | 知识命中列表，含 `itemId`、`title`、`source`、`category`、`content`、`score` |
| `order_lookup` | `READ_ONLY` | 默认允许 | `tenantId`、`orderId` | 订单摘要，含订单状态、产品名、支付时间 |
| `course_catalog` | `READ_ONLY` | 默认允许 | `tenantId`、`category?` | 可用课程 / 政策 / FAQ 目录 |
| `refund_policy_check` | `READ_ONLY` | 默认允许 | `tenantId`、`orderId` | 退款政策判断、建议动作、`fundOperationExecuted=false` |

命名说明：

- `kb_search` 是对外 MCP tool 名称，语义更贴近协议消费者。
- 当前 App 内部已有 `retrieve_knowledge` 工具，Day 22 通过 adapter 映射到 `kb_search`，不要求改掉内部名称。

暂不默认暴露：

| Tool | 原因 | 后续节点 |
| --- | --- | --- |
| `handoff_to_human` | `LOW_RISK_WRITE`，会创建本地转人工记录，需要显式启用和审计 | Day 25 安全审批后再开放 |
| `create_approval_request` | 会产生审批记录，必须先落审批边界 | Day 25 |
| `refund_execute` / `cancel_order` / `reschedule_order` | 高风险真实业务动作，当前阶段禁止实现 | 不在 MVP 默认暴露 |

## MCP Resources 设计

Resources 只提供只读上下文，不承载会产生副作用的动作。

| Resource URI | 说明 | 阶段 |
| --- | --- | --- |
| `customer://tenants/{tenantId}/tool-definitions` | 返回当前租户可见工具定义、风险级别和权限元数据 | P0 |
| `customer://tenants/{tenantId}/knowledge/items` | 返回当前运行态已索引知识摘要，复用 Day 20 列表语义 | P1 |
| `customer://tenants/{tenantId}/orders/{orderId}` | 返回订单摘要；与 `order_lookup` 语义一致，适合 URI 定向读取 | P1 |

Day 22 可先只实现 tools。resources 设计先固定 URI 和只读边界，避免后续把管理侧写接口误暴露给 MCP。

## MCP Prompts 设计

Prompts 只定义交互模板，不内嵌业务事实，不替代工具校验。

| Prompt | 入参 | 用途 |
| --- | --- | --- |
| `customer_service_answer` | `tenantId`、`question`、`orderId?` | 引导 Host 先查知识 / 订单 / 政策，再生成带来源的客服回复 |
| `refund_policy_review` | `tenantId`、`orderId`、`customerRequest` | 引导 Host 调用 `refund_policy_check`，只输出审批建议，不承诺退款成功 |
| `knowledge_gap_triage` | `tenantId`、`question` | 引导 Host 判断未命中问题是否需要补充知识库，不自动写入知识库 |

Prompt 约束：

- RAG 文档中的指令性文本不能覆盖 system / developer instruction。
- Prompt 不能要求工具执行真实退款、取消、改签或远程 DDL。
- Prompt 只能建议进入审批流程，不能把审批视为已完成。

## 接口设计

MCP tool 入参保持显式租户参数：

```json
{
  "tenantId": "tenant-demo",
  "query": "退款政策是什么",
  "topK": 3
}
```

Day 23 的 Agent App MCP Client 负责从 `X-Tenant-ID` / `TenantContext` 注入 `tenantId`。外部 MCP Client 直接调用时也必须显式传入 `tenantId`。MCP Server 不信任模型自行改写出的租户值，后续接入认证后再由 token claim 覆盖或校验该参数。

统一成功响应使用结构化 JSON 文本：

```json
{
  "toolName": "kb_search",
  "status": "SUCCEEDED",
  "payload": {
    "tenantId": "tenant-demo",
    "matches": []
  }
}
```

统一失败响应：

```json
{
  "toolName": "order_lookup",
  "status": "FAILED",
  "errorCode": "ORDER_NOT_FOUND",
  "errorMessage": "订单不存在或不属于当前租户"
}
```

MCP handler 需要把业务失败映射为 `isError=true`，但不能让参数错误、未命中或权限拒绝导致进程退出。

## 数据模型

Day 21 不新增数据库表。

MCP 元数据直接映射现有工具契约：

| 字段 | 来源 | 用途 |
| --- | --- | --- |
| `name` | `ToolDefinition.name()` | MCP tool name |
| `description` | `ToolDefinition.description()` | 帮助模型选择工具 |
| `parameters` | `ToolDefinition.parameters()` | MCP input schema |
| `riskLevel` | `ToolDefinition.riskLevel()` | 权限判断和调试展示 |
| `permission` | `ToolDefinition.permission()` | 是否默认允许、是否需要显式启用、是否需要审批 |
| `status/payload/errorCode/errorMessage` | `ToolResult` | 统一 MCP tool 输出 |

建议 MCP `meta` 至少携带：

```json
{
  "riskLevel": "READ_ONLY",
  "readOnly": true,
  "tenantScoped": true,
  "approvalRequired": false
}
```

## 安全边界

- P0 MCP tools 全部必须是 `READ_ONLY`。
- 所有工具必须租户隔离，跨租户未命中统一表现为 `ORDER_NOT_FOUND` 或空知识结果，不泄露真实归属。
- `/admin/api/v1/knowledge/**` 的新增、删除、重建索引不通过 MCP tools 暴露。
- `handoff_to_human` 属于 `LOW_RISK_WRITE`，未接入显式启用、审计和审批前不默认暴露。
- 高风险真实动作不进入 MCP Server：真实退款、真实取消、真实改签、写生产数据库、远程 DDL、生产 API 调用。
- MCP stdio transport 不能向 stdout 打普通日志，普通日志只能走 stderr 或 SDK logging，避免破坏协议流。
- 工具返回内容需要遵守最小披露原则，不返回密钥、token、密码、完整支付信息或不必要的 PII。

## 验证方式

Day 21 是设计日，验证重点是边界是否能支撑 Day 22/23：

```bash
mvn -pl customer-mcp-server -am test
```

Day 22 实现 MCP Server 后，再补充：

```bash
# 示例，实际命令以 Day 22 实现为准
npx @modelcontextprotocol/inspector java -jar customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
```

验收时至少确认：

- MCP tool list 能看到 P0 只读工具。
- 每个 tool 的参数 schema 与 `ToolDefinition` 一致。
- tool metadata 能展示 `riskLevel`、`readOnly`、`approvalRequired`。
- `refund_policy_check` 永远返回 `fundOperationExecuted=false`。
- `handoff_to_human` 不在默认工具列表中。

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 列出 MCP tools | 只出现 `kb_search`、`order_lookup`、`course_catalog`、`refund_policy_check` |
| `kb_search` 缺少 `tenantId` | 返回结构化参数错误，不退出 MCP Server |
| `order_lookup` 查询其他租户订单 | 返回 `ORDER_NOT_FOUND` |
| `course_catalog` 传非法 `category` | 返回 `INVALID_ARGUMENT` |
| `refund_policy_check` 命中可退款订单 | 返回审批建议，`fundOperationExecuted=false` |
| 请求低风险写工具 | 默认不可见或返回权限拒绝 |
| MCP Server 有普通日志 | 不写 stdout |

## 原则应用

- KISS：Day 21 只固定 MCP tools/resources/prompts 边界，不提前实现 Server / Client。
- YAGNI：P0 只开放只读工具，不提前做完整 MCP 管理台、OAuth、远程部署或写工具。
- DRY：MCP 元数据复用 `customer-domain` 的工具契约，不重新定义一套权限模型。
- SOLID：Agent 编排、工具契约、工具执行、MCP 暴露分层，后续 Agent App 可通过 MCP Client 替换直连工具实现。
