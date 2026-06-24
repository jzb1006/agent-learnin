# Day 12：最小 MCP Server

## 一句话定义

最小 MCP Server 是一个能完成 MCP 初始化、工具发现和工具调用的独立程序；本项目用 `ping -> pong` 验证最小协议闭环。

## 解决的问题

- 把工具能力从 `agent-app` 内部方法调用推进到 MCP 协议边界。
- 验证 stdio transport 可以连接本地 MCP Server。
- 验证 `tools/list` 能发现工具。
- 验证 `tools/call` 能调用工具并返回 MCP content。
- 为 Day 13-14 迁移真实只读工具提供启动和测试模板。

## 不解决的问题

- 不迁移 `search_code`、`git_history` 或 `read_config`。
- 不做 Resources、Prompts、Roots。
- 不做远程 HTTP MCP Server。
- 不做 Agent 自动规划或诊断报告生成。
- 不做任何写操作或高风险操作。

## 在本项目中的位置

```text
agent-app
  mcp.MinimalMcpClient
    -> initialize
    -> tools/list
    -> tools/call ping

mcp-server
  mcpserver.MinimalMcpServerApplication
    -> StdioServerTransportProvider
    -> ping tool
    -> pong
```

## 最小代码证据

- `mcp-server/pom.xml` 引入 MCP Java SDK，并通过 Maven Shade Plugin 生成可执行 JAR。
- `MinimalMcpServerApplication` 注册 `ping` 工具。
- `ping` 输入 schema 是空对象，拒绝额外参数。
- `ping` annotations 标记为只读、幂等、非破坏性。
- `MinimalMcpClient` 通过 stdio 启动 Server，初始化后调用 `listTools()` 和 `callTextTool("ping")`。
- `MinimalMcpClientTest` 验证 Agent 侧能发现并调用 `ping`。

## 常见误区

- 把进程启动成功等同于 MCP 协议可用。
- 在 stdio Server 的 stdout 写普通日志，破坏 JSON-RPC 协议流。
- 为了一个 `ping` 提前引入 HTTP 服务和认证。
- 把 `ping` 做成业务诊断工具。
- 忽略 Client 和 Server 的依赖版本收敛。

## 自测问题

1. `tools/list` 和 `tools/call` 分别验证了 MCP 链路里的哪一部分？
2. 为什么 `ping` 应该是只读、幂等、无参数工具？
3. stdio transport 的 stdout 和 stderr 在 MCP Server 中应该如何分工？

## 今日结论

Day 12 的核心是完成第一个 MCP 协议闭环：`agent-app` 不再直接调用工具实现，而是通过 MCP Client 发现并调用 `mcp-server` 暴露的 `ping` 工具。
