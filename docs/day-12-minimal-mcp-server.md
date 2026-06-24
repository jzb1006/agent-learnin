# Day 12：搭建最小 MCP Server

## 今日节点

Day 12：搭建最小 MCP Server。

## 今日主题

用 MCP Java SDK 搭建一个最小 stdio MCP Server，注册一个 `ping` 工具，并让 `agent-app` 通过 MCP Client 发现和调用它。

## 今天解决的问题

- 如何把 `mcp-server` 从 README 变成可启动的 MCP Server 程序。
- 如何注册一个最小 Tool。
- 如何通过 `tools/list` 发现 `ping`。
- 如何通过 `tools/call` 调用 `ping` 并拿到 `pong`。
- 如何用 stdio transport 把 `agent-app` 和 `mcp-server` 接起来。
- 为什么 stdio MCP Server 的 stdout 不能写普通日志。

## 今天不解决的问题

- 不迁移 `search_code`，放到 Day 13。
- 不迁移 `git_history` 和 `read_config`，放到 Day 14。
- 不设计高风险写工具，放到 Day 15 以后只做权限分类和审批边界。
- 不实现 Resources、Prompts、Roots，放到 Day 16-18。
- 不引入 Streamable HTTP、远程部署或生产鉴权。

## 一句话定义

最小 MCP Server 是一个能通过标准 transport 完成初始化、暴露工具列表并响应工具调用的独立程序；Day 12 只用 `ping -> pong` 验证协议闭环。

## 概念讲解

### MCP Server 解决什么

Day 09-10 的工具还在 `agent-app` 内部，Agent 可以直接 new 一个 Java 类调用。

Day 12 开始，工具能力要进入协议边界：

```text
agent-app
  -> MinimalMcpClient
     -> stdio transport
        -> mcp-server
           -> ping
```

这一步的重点不是 `ping` 本身，而是验证：

- Server 可以启动。
- Client 可以初始化连接。
- Client 可以通过 `tools/list` 看到工具。
- Client 可以通过 `tools/call` 调用工具。
- 工具返回符合 MCP content 格式。

### MCP Server 不解决什么

MCP Server 不负责理解用户问题，也不负责决定什么时候调用 `ping`。

在本项目里：

- Agent 编排仍属于 `agent-app`。
- 工具执行边界属于 `mcp-server`。
- 最终诊断报告仍由 Agent 汇总证据后生成。

### 和 Java 后端的类比

`ping` 工具类似后端服务里的第一个 health endpoint。

但它不是 HTTP health check，而是 MCP 协议级 health check：

```text
HTTP health:
  GET /actuator/health -> UP

MCP ping tool:
  initialize -> tools/list -> tools/call ping -> pong
```

`/actuator/health` 只能说明进程还活着；`tools/list + tools/call` 能说明协议、工具注册和调用链都通了。

## 项目映射

Day 12 新增代码位置：

```text
projects/mcp-troubleshooting-agent/
  mcp-server/
    pom.xml
    src/main/java/io/github/jiangzhibin/agentlearning/mcpserver/
      MinimalMcpServerApplication.java
    src/test/java/io/github/jiangzhibin/agentlearning/mcpserver/
      MinimalMcpServerApplicationTest.java

  agent-app/
    src/main/java/io/github/jiangzhibin/agentlearning/mcp/
      MinimalMcpClient.java
    src/test/java/io/github/jiangzhibin/agentlearning/mcp/
      MinimalMcpClientTest.java
```

职责边界：

| 模块 | 新增能力 | 不做什么 |
| --- | --- | --- |
| `mcp-server` | 注册 `ping`，通过 stdio 暴露 MCP Server | 不生成诊断报告，不读取业务项目 |
| `agent-app` | 通过 stdio MCP Client 做工具发现和调用 | 不实现 `ping` 逻辑，不直接访问工具内部 |

## 代码证据

### `mcp-server`

`MinimalMcpServerApplication` 做了三件事：

- 创建 `StdioServerTransportProvider`。
- 注册只读、幂等、非破坏性的 `ping` 工具。
- 在 `ping` 被调用时返回文本内容 `pong`。

`ping` 的输入 schema 是空对象：

```text
type: object
properties: {}
required: []
additionalProperties: false
```

这表示 Day 12 的 `ping` 不接收任何参数。额外参数会被拒绝。

### `agent-app`

`MinimalMcpClient` 做了三件事：

- 用 `ServerParameters` 启动 `mcp-server` JAR。
- 初始化 MCP Client。
- 暴露 `listTools()` 和 `callTextTool("ping")` 两个最小方法。

为了便于排错，`MinimalMcpClient` 会收集服务端 stderr。Server JAR 不存在、主类缺失或依赖缺失时，初始化异常里能看到服务端错误信息。

## 运行验证

先打包 MCP Server：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" package
```

再运行 Agent 侧 stdio MCP 集成测试：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test -Dtest=MinimalMcpClientTest
```

完整回归：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" test
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test
```

## 今日踩坑

### 1. MCP SDK 2.0.0 和 Jackson 注解版本冲突

`agent-app` 原来直接依赖 Jackson 2.17.2。MCP Java SDK 2.0.0 的 `mcp-core` 需要 `jackson-annotations:2.20`，但 Maven 最近优先导致 `jackson-annotations:2.17.2` 覆盖了它。

实际症状：

```text
NoSuchMethodError: com.fasterxml.jackson.annotation.JsonProperty.isRequired()
```

处理方式：

- 将 `agent-app` 的 Jackson 2 版本统一升级到 `2.20.0`。
- 保持 Jackson 2 仍服务现有结构化输出代码。
- MCP SDK 内部继续使用 Jackson 3 mapper。

### 2. stdio 集成测试必须先有可执行 Server JAR

只跑 `mcp-server test` 不会证明 `java -jar mcp-server-0.1.0-SNAPSHOT.jar` 可启动。

因此 `mcp-server` 增加了 Maven Shade Plugin：

- 打包依赖。
- 写入 `Main-Class`。
- 生成 Agent 侧测试可直接启动的 JAR。

### 3. stdout 只能写 MCP 协议消息

stdio MCP Server 通过 stdin/stdout 传输 JSON-RPC 消息。普通日志如果写到 stdout，会污染协议流。

Day 12 的 server 入口没有向 stdout 打业务日志。后续需要调试时，应写 stderr 或使用 SDK 日志能力。

## 工程原则说明

- KISS：只做 `ping`，不提前迁移真实排障工具。
- YAGNI：不引入 HTTP transport、认证或多客户端会话。
- DRY：Agent 侧工具发现和工具调用统一走 `MinimalMcpClient`，不在测试里手写 JSON-RPC。
- SOLID：`agent-app` 依赖 MCP Client 边界，`mcp-server` 负责工具注册和执行，职责分离。

## 复盘提问

1. 为什么 `ping` 工具比单纯看 Java 进程是否启动更能证明 MCP 链路可用？
2. 为什么 stdio MCP Server 不能用 `System.out.println` 打普通日志？
3. 为什么 Day 12 只做 `ping`，而不是顺手把 `search_code` 一起迁移？

## 验收标准

完成 Day 12 时，你应该能验证：

- `mcp-server` 能打包成可执行 JAR。
- `agent-app` 能通过 MCP Client 初始化 stdio 连接。
- `agent-app` 能通过 `tools/list` 发现 `ping`。
- `agent-app` 能通过 `tools/call` 调用 `ping` 并得到 `pong`。
- `ping` 是只读、幂等、无副作用工具。
