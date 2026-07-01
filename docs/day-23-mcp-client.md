# Day 23：实现 MCP Client

## 目标

让 Agent App 通过 MCP Client 边界调用客服工具：

```text
/chat
-> ChatService
-> McpToolClient
-> StdioMcpToolClient
-> customer-mcp-server
-> ToolResult
```

Day 23 的重点是把 Agent 编排层从具体工具实现类上解耦出来，并补上真实 stdio MCP service 调用闭环。清理后，Agent App 生产代码不再保留本地工具实现或 local adapter；默认通过 MCP SDK 启动 `customer-mcp-server` JAR，并执行 `initialize`、`tools/list` 和 `tools/call`。

## 业务场景

客服对话中，Agent App 需要根据意图调用工具：

- 知识问答：调用 `kb_search` 检索 FAQ、政策和产品知识。
- 订单查询：调用 `order_lookup` 查询订单摘要。
- 退款或取消咨询：调用 `refund_policy_check` 做政策前置判断。

这三个入口都保持只读，不执行真实退款、真实取消、真实改签或外部工单派发。

## 模块边界

| 模块 | Day 23 职责 |
| --- | --- |
| `customer-agent-app` | 保留 MCP client 抽象和 stdio client，让 `ChatService` 只依赖 `McpToolClient` |
| `customer-domain` | 继续提供 `ToolDefinition`、`ToolResult`、风险级别和权限模型 |
| `customer-mcp-server` | Day 22 已实现 MCP Server；Day 23 通过 stdio 进程和协议调用，不做 Java 模块依赖 |

依赖方向保持：

```text
customer-domain <- customer-agent-app
customer-domain <- customer-mcp-server
```

`customer-agent-app` 不依赖 `customer-mcp-server` 的 Java 模块，也不保留 `agent.tool.*` 业务工具实现。工具执行唯一生产归属是 `customer-mcp-server`；Agent App 只负责 Host / Client / 编排。

真实 MCP service 模式下的运行链路是：

```text
ChatService
-> McpToolClient
-> StdioMcpToolClient
-> MCP Java SDK StdioClientTransport
-> java -jar customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
-> MCP tools/list + tools/call
-> ToolResult JSON
```

## 接口设计

新增 MCP client 边界：

| 类型 | 说明 |
| --- | --- |
| `McpToolClient` | Agent App 侧发现和调用 MCP 工具的接口 |
| `McpToolCallRequest` | 工具名和参数 map |
| `McpToolCallResponse` | 工具名、`ToolResult` 和耗时 |
| `McpToolNames` | 集中保存 MCP 对外工具名 |
| `StdioMcpToolClient` | 通过 MCP Java SDK 启动并调用真实 `customer-mcp-server` 进程 |

当前可发现工具固定为 Day 22 的 P0 只读集合：

| Tool | 参数 | 执行归属 |
| --- | --- | --- |
| `kb_search` | `tenantId`、`query`、`topK?` | `customer-mcp-server` |
| `order_lookup` | `tenantId`、`orderId` | `customer-mcp-server` |
| `course_catalog` | `tenantId`、`category?` | `customer-mcp-server` |
| `refund_policy_check` | `tenantId`、`orderId` | `customer-mcp-server` |

`ChatService` 现在只构造 `McpToolCallRequest`，再使用 `McpToolClient.call(...)` 获取 `ToolResult`。`CustomerAgentToolCall.name` 对外展示 MCP 工具名，例如知识问答路径从内部实现名 `retrieve_knowledge` 收敛为 MCP 工具名 `kb_search`。

配置方式：

```yaml
customer-agent:
  mcp-client:
    mode: stdio
    command: java
    server-jar: ../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
    request-timeout-seconds: 10
```

对应环境变量：

```bash
CUSTOMER_AGENT_MCP_CLIENT_MODE=stdio
CUSTOMER_AGENT_MCP_CLIENT_COMMAND=java
CUSTOMER_AGENT_MCP_CLIENT_SERVER_JAR=../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
CUSTOMER_AGENT_MCP_CLIENT_REQUEST_TIMEOUT_SECONDS=10
```

默认和集成环境都启用 stdio：

```bash
SPRING_PROFILES_ACTIVE=integration
```

普通单元测试如需隔离 MCP 子进程，使用 `src/test/java` 下的 test-only fake `McpToolClient`，不在生产代码中恢复 local adapter。

## 数据模型

Day 23 不新增数据库表。

新增的数据结构都是 Java record / interface：

```text
McpToolCallRequest(toolName, arguments)
McpToolCallResponse(toolName, result, durationMs)
```

工具结果仍复用 `ToolResult`：

- 成功：`ToolResult.succeeded(toolName, payload)`
- 失败：`ToolResult.failed(toolName, errorCode, errorMessage)`

未知工具不会抛出运行期异常，而是返回：

```text
errorCode = MCP_TOOL_NOT_FOUND
```

## 安全边界

- Day 23 默认只把 P0 只读工具放进 `McpToolClient.listTools()`。
- `handoff_to_human` 仍不通过 MCP Client 默认暴露。
- `refund_policy_check` 仍只返回政策判断，`fundOperationExecuted=false`。
- `ChatService` 不允许模型绕过 Java 路由和风险级别决策。
- `customer-agent-app` 不把 `customer-mcp-server` 作为编译期依赖，避免应用层绕过 MCP 协议直接调服务端实现。
- stdio 模式要求 server stdout 只承载 MCP 协议，普通日志写文件或 stderr，避免破坏 JSON-RPC 流。
- 真实退款、真实取消、真实改签、写生产数据库、远程 DDL 和生产 API 调用仍不实现。

## 验证方式

定向验证 App 侧不再发布本地工具实现：

```bash
mvn -pl customer-agent-app -am -Dtest=McpToolOwnershipTest -Dsurefire.failIfNoSpecifiedTests=false test
```

定向验证真实 stdio MCP service 调用：

```bash
mvn -pl customer-mcp-server -am package
mvn -pl customer-agent-app -am -Dtest=StdioMcpToolClientIntegrationTest,StdioMcpToolClientApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

其中 `StdioMcpToolClientApplicationIntegrationTest` 只激活 `integration` profile，不再显式覆盖 `customer-agent.mcp-client.mode`，用于证明集成环境默认走真实 MCP service。

定向验证 `ChatService` 已通过 MCP client 调用工具：

```bash
mvn -pl customer-agent-app -am -Dtest=ChatServiceModelClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

定向验证 API 响应仍稳定：

```bash
mvn -pl customer-agent-app -am -Dtest=CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

全量验证：

```bash
mvn test
npm run build
git diff --check
```

本机如果没有全局 `mvn`，可使用 SDKMAN Maven 绝对路径：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/current/bin/mvn test
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| `McpToolOwnershipTest` | `customer-agent-app` 生产 classpath 不包含 `LocalMcpToolClient` 和 `agent.tool.*` 业务工具 |
| `StdioMcpToolClient.listTools()` | 启动真实 MCP Server 进程并发现 `kb_search`、`order_lookup`、`course_catalog`、`refund_policy_check` |
| `StdioMcpToolClient.call(order_lookup)` | 通过 MCP `tools/call` 返回订单 `ToolResult` |
| `mode=stdio` 的 Spring 上下文 | `ChatService` 注入 `StdioMcpToolClient` 并通过真实 MCP Server 完成订单查询 |
| `ChatService` 构造函数依赖 | 包含 `McpToolClient`，不包含 `agent.tool.*` 业务工具 |
| `/chat` 知识问答 | `toolCalls[0].name = kb_search` |
| `/chat` 订单查询 | 仍返回订单来源和只读风险级别 |
| `/chat` 退款咨询 | 仍只做政策检查，不执行真实退款 |

## 原则应用

- KISS：生产代码只保留 stdio MCP transport，不保留重复 local adapter。
- YAGNI：不提前实现 SSE / HTTP MCP client、认证、多 server registry，不扩展写工具和高风险动作。
- DRY：继续复用现有 `ToolDefinition`、`ToolResult` 和工具 payload，不重新定义结果模型。
- SOLID：`ChatService` 依赖 `McpToolClient` 抽象，具体工具执行归属 `customer-mcp-server`；后续替换 transport 不需要修改对话编排层。
