# Day 22：实现 MCP Server

## 目标

实现客服订单 MCP Server 的最小可运行闭环：

```text
MCP Host / Inspector
-> customer-mcp-server
-> 4 个只读客服工具
-> 统一 ToolResult JSON
```

Day 22 只实现 Server 侧 tools 暴露，不改 `customer-agent-app` 当前 `/chat` 直连工具路径。Agent App 通过 MCP Client 调用工具留到 Day 23。

## 业务场景

外部 MCP Host 需要发现并调用客服订单平台的只读能力：

- 查询 FAQ、政策和产品知识。
- 查询订单摘要。
- 查询课程 / 政策 / FAQ 目录。
- 检查退款政策并给出审批建议。

退款政策检查只做规则判断，不能执行真实退款、取消、改签或任何资金操作。

## 模块边界

| 模块 | Day 22 职责 |
| --- | --- |
| `customer-domain` | 继续提供 `ToolDefinition`、`ToolResult`、风险级别和权限模型 |
| `customer-mcp-server` | Spring Boot MCP Server 启动入口、MCP tool 注解、只读 demo 数据和工具 facade |
| `customer-agent-app` | 暂不接 MCP Client，继续保留现有本地工具调用路径 |

依赖方向仍保持：

```text
customer-domain <- customer-mcp-server
customer-domain <- customer-agent-app
```

`customer-mcp-server` 不依赖 `customer-agent-app`，避免 MCP Server 直接调用 Web Controller 或 App 启动类。

## 接口设计

Day 22 暴露 4 个 P0 MCP tools：

| Tool | 参数 | 返回 |
| --- | --- | --- |
| `kb_search` | `tenantId`、`query`、`topK?` | `matches`，含 `itemId`、`title`、`source`、`category`、`content`、`score` |
| `order_lookup` | `tenantId`、`orderId` | 订单摘要，含订单状态、产品名、支付时间 |
| `course_catalog` | `tenantId`、`category?` | 当前租户可见课程 / 政策 / FAQ 目录 |
| `refund_policy_check` | `tenantId`、`orderId` | 退款政策判断、建议动作、`fundOperationExecuted=false` |

统一成功响应复用 `ToolResult.succeeded`：

```json
{
  "toolName": "order_lookup",
  "status": "SUCCEEDED",
  "payload": {}
}
```

统一失败响应复用 `ToolResult.failed`：

```json
{
  "toolName": "order_lookup",
  "status": "FAILED",
  "payload": {},
  "errorCode": "ORDER_NOT_FOUND",
  "errorMessage": "订单不存在或不属于当前租户"
}
```

## 实现要点

- `CustomerMcpServerApplication`：Spring Boot MCP Server 启动入口。
- `CustomerMcpTools`：承载 `@McpTool` 注解方法，暴露 4 个只读工具。
- `CustomerMcpToolCatalog`：固定默认工具目录，防止写工具被默认发现。
- `CustomerMcpKnowledgeItem`：Day 22 内存 demo 知识条目。
- `maven-compiler-plugin`：为 MCP 模块启用 `-parameters`，确保 Spring AI MCP 生成 `tenantId`、`orderId` 等业务参数名，而不是 `arg0`、`arg1`。
- `application.yml`：启用 MCP Server STDIO，关闭 banner，保持非 Web 应用。
- `logback-spring.xml`：普通日志写入文件，避免污染 MCP STDIO stdout 协议流。

## 数据模型

Day 22 不新增数据库表。

当前 MCP Server 使用内存 demo 数据验证协议和工具边界：

- `tenant-demo` 的 FAQ、政策、产品知识。
- `tenant-demo` 的 mock 订单。

后续替换方向：

- Day 23：Agent App 通过 MCP Client 调用这些工具。
- 后续数据库阶段：把 demo 数据替换为共享查询用例或仓储 adapter。

## 安全边界

- 默认只暴露 `READ_ONLY` tools。
- `handoff_to_human` 不在默认 MCP tool 列表中。
- `refund_policy_check` 永远返回 `fundOperationExecuted=false`。
- 跨租户订单查询统一返回 `ORDER_NOT_FOUND`，不泄露真实归属。
- 非法分类返回 `INVALID_ARGUMENT`。
- 真实退款、真实取消、真实改签、写生产数据库、远程 DDL、生产 API 调用都不通过 MCP Server 暴露。

## 验证方式

单模块验证：

```bash
mvn -pl customer-mcp-server -am test
```

定向测试：

```bash
mvn -pl customer-mcp-server -am -Dtest=CustomerMcpToolCatalogTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl customer-mcp-server -am -Dtest=CustomerMcpToolsTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl customer-mcp-server -am -Dtest=CustomerMcpToolSpecificationTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl customer-mcp-server -am -Dtest=CustomerMcpServerApplicationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

MCP Inspector 示例：

```bash
mvn -pl customer-mcp-server -am package
npx @modelcontextprotocol/inspector \
  java -jar customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
```

本机如果没有全局 `mvn`，可使用本机 SDKMAN Maven 绝对路径：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/3.9.10/bin/mvn -pl customer-mcp-server -am test
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 列出默认工具目录 | 只包含 `kb_search`、`order_lookup`、`course_catalog`、`refund_policy_check` |
| MCP 注解扫描 | 只标注 4 个 P0 tools，不包含 `handoff_to_human` |
| MCP SDK tool specs | 能生成 4 个 tool specifications，schema 使用业务参数名 |
| MCP SDK call handler | 能通过 `CallToolRequest` 调用 `order_lookup` 并返回 JSON 文本 |
| `kb_search` 正常查询 | 返回结构化 matches |
| `kb_search` 缺少 `tenantId` | 返回 `INVALID_ARGUMENT` |
| `order_lookup` 查询其他租户订单 | 返回 `ORDER_NOT_FOUND` |
| `course_catalog` 传非法 `category` | 返回 `INVALID_ARGUMENT` |
| `refund_policy_check` 命中可退款订单 | 返回 `CREATE_APPROVAL_REQUEST` 且 `fundOperationExecuted=false` |
| Spring Boot 上下文启动 | 能加载 `CustomerMcpTools` bean |

## 原则应用

- KISS：只实现 STDIO MCP Server 和 4 个 P0 只读工具，不扩展 HTTP/SSE、OAuth 或完整管理台。
- YAGNI：不提前实现 MCP Client、审批写工具和真实业务写操作。
- DRY：复用 `customer-domain` 的 `ToolDefinition` / `ToolResult` / 风险级别模型。
- SOLID：工具目录、工具执行、启动入口、日志配置分离；`customer-agent-app` 与 `customer-mcp-server` 仍通过领域契约解耦。
