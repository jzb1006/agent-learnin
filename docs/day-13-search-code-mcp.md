# Day 13：把 search_code 接入 MCP

## 今日节点

Day 13：把 `search_code` 接入 MCP。

## 今日主题

把 Day 09 的本地代码搜索能力迁移到 MCP Server，让 `agent-app` 通过 MCP Client 调用 `search_code`，而不是直接依赖本地工具实现。

## 今天解决的问题

- 如何把真实只读工具注册为 MCP Tool。
- 如何让工具 schema 和 Day 09 的 `search_code` 契约对齐。
- 如何让 MCP Server 自己管理允许搜索根目录。
- 如何把工具结果序列化成 Agent 可消费的 JSON 文本。
- 如何让 `agent-app` 通过 stdio MCP 调用带参数工具。
- 为什么 Agent 不应该把目标项目路径作为工具参数交给模型决定。

## 今天不解决的问题

- 不迁移 `git_history` 和 `read_config`，放到 Day 14。
- 不设计工具权限分类元数据，放到 Day 15。
- 不实现 MCP Resources、Prompts、Roots，放到 Day 16-18。
- 不引入远程 HTTP MCP Server、认证或多客户端连接。
- 不做 Agent 自动选择工具和多步编排，后续进入 Agent loop 时再处理。

## 一句话定义

把 `search_code` 接入 MCP，就是把代码搜索从 Agent 内部 Java 方法调用迁移为标准 MCP 工具调用：Agent 只看工具名称、schema 和结果，具体搜索逻辑留在 MCP Server。

## 概念讲解

### MCP Tool 迁移解决什么

Day 09 的调用方式是：

```text
agent-app
  -> LocalCodeSearchTool
     -> Files.walk(allowedRoot)
```

Day 13 的调用方式变成：

```text
agent-app
  -> MinimalMcpClient
     -> stdio transport
        -> mcp-server
           -> search_code
              -> Files.walk(allowedRoot)
```

迁移后，Agent 不再依赖 `LocalCodeSearchTool` 的具体类，也不需要知道搜索实现是本地文件、远程服务还是测试替身。

### MCP schema 解决什么

MCP Tool 的 schema 是 Agent 选择和调用工具时看到的契约。

`search_code` 的 schema 只暴露一个参数：

```text
keyword: string
```

这表示模型只能决定“搜什么”，不能决定“去哪搜”。允许根目录由 MCP Server 启动环境配置。

### MCP Tool 不解决什么

MCP 不会自动带来安全边界。

即使通过 MCP 调用，Server 仍然必须自己做：

- 参数校验。
- 允许根目录规范化。
- 符号链接防护。
- 结果数量裁剪。
- 错误语义区分。

## 项目映射

Day 13 新增和修改代码位置：

```text
projects/mcp-troubleshooting-agent/
  mcp-server/
    src/main/java/io/github/jiangzhibin/agentlearning/mcpserver/
      MinimalMcpServerApplication.java
      SearchCodeMcpTool.java
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
| `mcp-server` | 注册并执行 `search_code`，管理允许根目录和路径边界 | 不理解用户问题，不生成诊断报告 |
| `agent-app` | 通过 MCP Client 发现并调用带参数工具 | 不直接依赖 `LocalCodeSearchTool`，不读取目标项目源码 |

## 代码证据

### `mcp-server`

`MinimalMcpServerApplication` 继续保留 `ping`，同时注册 `search_code`。

`SearchCodeMcpTool` 负责：

- 校验允许根目录存在且是目录。
- 只接收 `keyword` 参数。
- 拒绝空关键词、超长关键词和未知参数。
- 只扫描 `.java` 文件。
- 不跟随符号链接。
- 只返回前 5 个匹配片段。
- 返回包含 `status / summary / evidence` 的 JSON 文本。

`search_code` 的输入 schema：

```text
type: object
properties:
  keyword:
    type: string
    minLength: 1
    maxLength: 128
required:
  - keyword
additionalProperties: false
```

工具 annotations：

```text
readOnlyHint: true
idempotentHint: true
destructiveHint: false
openWorldHint: false
```

### `agent-app`

`MinimalMcpClient` 扩展了两点：

- 支持给 MCP Server 子进程传环境变量。
- 支持 `callTextTool(toolName, arguments)` 调用带参数工具。

Agent 侧测试通过如下环境变量设置 Server 的允许搜索根目录：

```text
MCP_SEARCH_CODE_ROOT=<tempDir>
```

模型或 Agent 调用 `search_code` 时只传：

```json
{
  "keyword": "HikariPool"
}
```

## 运行验证

先打包 MCP Server，确保 Agent 集成测试启动的是最新可执行 JAR：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" package
```

运行 Server 单元测试：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" test -Dtest=MinimalMcpServerApplicationTest
```

运行 Agent 侧 stdio MCP 集成测试：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test -Dtest=MinimalMcpClientTest
```

完整回归：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" test
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test
```

## 今日踩坑

### 1. 集成测试必须先重新打包 Server JAR

Agent 侧集成测试启动的是：

```text
../mcp-server/target/mcp-server-0.1.0-SNAPSHOT.jar
```

如果只编译或只跑 Server 单测，不重新 `package`，这个 JAR 可能仍是 Day 12 旧版本。实际症状是 Agent 侧 `tools/list` 看不到 `search_code`，调用时返回：

```text
Tool not found: search_code
```

处理方式是先执行 `mcp-server package`，再跑 `agent-app` 的 stdio 集成测试。

### 2. root 不应该出现在工具参数里

直觉上可以让 `search_code` 接收：

```json
{
  "root": "...",
  "keyword": "..."
}
```

但这会把访问边界交给模型输入，风险过高。

Day 13 的决策是：

- `root` 属于 Server 运行配置。
- `keyword` 属于工具调用参数。
- Agent 只能决定搜索关键词，不能决定搜索边界。

### 3. MCP 返回格式不是 Java 对象

MCP Tool 返回的是 `CallToolResult`，内容通常是 `content: [{ type: "text", text: "..." }]`。

因此 `search_code` 把工具结果序列化成 JSON 文本，而不是直接返回 Java record。Agent 后续可以在编排层把 JSON 文本解析回统一工具结果语义。

## 工程原则说明

- KISS：只迁移 `search_code`，不顺手迁移 Day 14 的两个工具。
- YAGNI：不提前抽共享工具模块，不做远程 MCP Server。
- DRY：Agent 侧仍统一走 `MinimalMcpClient`，不在测试里手写 JSON-RPC。
- SOLID：Agent 依赖 MCP 协议边界，搜索执行职责留在 MCP Server。

## 复盘提问

1. 为什么 `search_code` 的 MCP schema 只暴露 `keyword`，而不暴露 `root`？
2. 为什么 Agent 侧集成测试需要先重新打包 `mcp-server` JAR？
3. MCP 化以后，`agent-app` 和 `mcp-server` 的职责边界分别是什么？

## 验收标准

完成 Day 13 时，你应该能验证：

- `mcp-server` 的 `tools/list` 能看到 `search_code`。
- `search_code` 的 schema 只允许 `keyword` 参数。
- `search_code` 是只读、幂等、非破坏性工具。
- `agent-app` 能通过 MCP Client 调用 `search_code`。
- `agent-app` 不再直接依赖本地代码搜索实现来完成 MCP 代码搜索。
