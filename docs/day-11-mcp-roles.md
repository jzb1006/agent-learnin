# Day 11：理解 MCP 的角色

## 今日节点

Day 11：理解 MCP 的角色。

## 今日主题

从 Day 08-10 的本地 `TroubleshootingTool` 过渡到 MCP：理解 MCP Host、MCP Client、MCP Server、tool list、tool call 和 transport 在排障 Agent 中分别负责什么。

## 今天解决的问题

- MCP 为什么不是“又一种 Tool Calling 写法”。
- MCP Host、MCP Client、MCP Server 的职责边界。
- `tools/list` 和 `tools/call` 在 Agent 调用链中的位置。
- stdio 和 Streamable HTTP transport 分别适合什么场景。
- 为什么后续要把 `search_code`、`git_history`、`read_config` 从 `agent-app` 迁移到 `mcp-server`。
- 为什么工具能力不应该长期直接写进 Agent 编排层。

## 今天不解决的问题

- 不搭建真实 MCP Server，放到 Day 12。
- 不注册 `ping` 工具，放到 Day 12。
- 不迁移 `search_code`，放到 Day 13。
- 不迁移 `git_history` 和 `read_config`，放到 Day 14。
- 不实现 MCP Resources、Prompts、Roots，放到 Day 16-18。
- 不做任何重启、部署、改配置、写数据库或生产 API 调用。

## 一句话定义

MCP 是 Agent 应用和外部上下文能力之间的标准协议：Agent 侧通过 MCP Client 发现并调用 MCP Server 暴露的工具、资源和提示词模板，而不是直接依赖具体工具实现。

## 概念讲解

### MCP 解决什么

Day 09-10 里，`agent-app` 已经直接持有本地工具实现：

```text
agent-app
  -> LocalCodeSearchTool
  -> LocalGitHistoryTool
  -> LocalConfigReadTool
```

这种方式能快速验证 Tool Calling，但长期会有几个问题：

- Agent 编排层知道太多工具实现细节。
- 工具能力难以被其他 Agent 或客户端复用。
- 工具注册、参数 schema、错误语义和权限元数据缺少协议级边界。
- 本地工具和远程工具的连接方式不统一。
- 后续做 contract test、Inspector 调试和权限分类时会混在应用内部。

MCP 的作用是把“工具能力”变成一个独立服务边界：

```text
agent-app
  -> MCP Client
     -> MCP Server
        -> search_code
        -> git_history
        -> read_config
```

Agent 只关心“有哪些工具、schema 是什么、调用结果是什么”，不直接关心工具内部怎么读文件、怎么跑 Git、怎么脱敏。

### MCP 不解决什么

MCP 不替代 LLM，也不替代 Agent 编排。

它不负责：

- 决定用户问题应该查哪些证据。
- 判断排障是否已经收敛。
- 生成最终诊断报告。
- 替 Agent 做长期记忆、RAG、上下文压缩。
- 自动保证工具安全。

MCP Server 仍然必须自己做输入校验、路径边界、敏感信息脱敏、错误隔离和权限标记。

### 和 Java 后端开发的类比

直接在 Agent 里写工具，类似 Controller 里直接写 SQL、文件读取和 Git 命令。

引入 MCP 后，更像把能力拆成一个明确的后端服务契约：

```text
Agent 编排层
  -> MCP Client SDK
     -> JSON-RPC 协议
        -> MCP Server
           -> 工具实现
```

这和 Java 服务之间通过 REST/gRPC 调用类似。差异是 MCP 面向 AI 应用，协议里内建了工具发现、工具调用、资源读取、提示词模板和能力协商。

## 关键角色

| 角色 | 一句话职责 | 本项目映射 |
| --- | --- | --- |
| MCP Host | 管理一个或多个 MCP Client 的 AI 应用 | `agent-app` 未来承担 Host 角色 |
| MCP Client | 与某一个 MCP Server 保持连接，负责协议交互 | `agent-app` 内部的 MCP client 组件 |
| MCP Server | 暴露工具、资源和提示词模板的独立程序 | `mcp-server` 模块 |
| Tool | 可被 AI 应用主动调用的函数 | `search_code` / `git_history` / `read_config` |
| Resource | 可读取的上下文数据 | 项目元信息、接口文档摘要、配置摘要 |
| Prompt | 可复用的交互模板 | 常见排障流程模板 |
| Transport | Client 与 Server 的通信方式 | 本地优先 stdio，后续可评估 Streamable HTTP |

容易混淆的一点：MCP Client 不是模型，也不是 Agent 本身。它是 Host 内部负责连接某个 MCP Server 的协议组件。

## 调用链

### 1. 初始化

```text
agent-app 启动
  -> 创建 MCP Client
  -> 连接 mcp-server
  -> initialize 能力协商
  -> 记录 server 支持 tools/resources/prompts
```

初始化阶段要确认协议版本、双方能力和服务端身份。没有初始化完成，不应该直接调用工具。

### 2. 工具发现：tools/list

```text
MCP Client
  -> tools/list
     -> MCP Server 返回工具列表
        -> search_code(inputSchema)
        -> git_history(inputSchema)
        -> read_config(inputSchema)
```

`tools/list` 的价值是让 Agent 不需要把工具列表写死在 Prompt 或代码分支里。工具是否存在、参数 schema 和描述都来自 Server。

### 3. 工具调用：tools/call

```text
用户问题
  -> Agent 判断需要查源码
  -> 模型选择 search_code 并生成参数
  -> Agent 校验工具名和参数
  -> MCP Client 发送 tools/call
  -> MCP Server 执行 search_code
  -> 返回结构化结果
  -> Agent 把结果作为 observation
  -> 继续查证或生成报告
```

在本项目里，`tools/call` 的结果仍应转换成类似 Day 08 的 `ToolResult` 语义：成功证据、参数错误、权限拒绝、执行失败要分清。

## Transport 选择

| Transport | 特点 | 适合场景 | 本项目当前取舍 |
| --- | --- | --- | --- |
| stdio | Client 启动 Server 子进程，通过 stdin/stdout 交换 JSON-RPC 消息 | 本地开发、本地只读工具、无网络开销 | Day 12 优先采用 |
| Streamable HTTP | Server 独立运行，通过 HTTP POST/GET 和可选 SSE 通信 | 远程 MCP Server、多客户端连接、标准鉴权 | 后续部署阶段再评估 |

stdio 的一个重要工程规则：stdout 只能写 MCP 协议消息，日志应写 stderr 或使用 SDK 日志能力，否则会破坏协议流。

## 项目映射

当前状态：

```text
projects/mcp-troubleshooting-agent/
  agent-app/
    tool/
      TroubleshootingTool
      LocalCodeSearchTool
      LocalGitHistoryTool
      LocalConfigReadTool
  mcp-server/
    README.md
```

Day 11 后的目标心智模型：

```text
agent-app
  负责：
    用户问题理解
    调用计划
    MCP Client 连接
    observation 管理
    诊断报告生成

mcp-server
  负责：
    工具注册
    参数 schema
    输入校验
    allowedRoot 边界
    只读工具执行
    敏感值脱敏
    结构化结果返回
```

## 为什么工具不直接写进 Agent

短期直接写进 Agent 可以降低学习成本，Day 09-10 就是这样做的。进入 MCP 阶段后要拆出来，原因是：

- 单一职责：Agent 负责编排和判断，MCP Server 负责能力暴露和边界保护。
- 可替换：Agent 不依赖 `LocalCodeSearchTool` 这种具体类，后续可以换成本地 MCP、远程 MCP 或测试 MCP。
- 可发现：工具列表和 schema 来自 `tools/list`，减少硬编码。
- 可测试：可以对 MCP 协议做 contract test，而不是只测 Java 方法。
- 可复用：同一组排障工具可以被不同 Host 使用。
- 可治理：权限、风险等级、只读标记和审计更适合放在工具服务边界。

这不是为了复杂而复杂。MCP 是从“本地工具能跑”走向“工具能力可复用、可测试、可治理”的边界。

## 今日决策

Day 11 只建立 MCP 角色和调用链模型，不写生产代码。

原因：

- KISS：先理解协议边界，再搭最小 Server。
- YAGNI：今天不提前迁移三个真实工具，避免把角色理解和实现细节混在一起。
- DRY：后续 `search_code`、`git_history`、`read_config` 统一经 MCP 暴露，不各自设计一套调用协议。
- SOLID：Agent 编排层依赖 MCP 工具抽象，不依赖具体本地工具类。

## 和后续 Day 的关系

```text
Day 08：定义本地工具调用契约
Day 09：实现 search_code 本地工具
Day 10：实现 git_history / read_config 本地工具
Day 11：理解 MCP 角色和调用链
Day 12：搭建最小 MCP Server，暴露 ping
Day 13：把 search_code 接入 MCP
Day 14：把 git_history / read_config 接入 MCP
Day 15：补工具权限分类
Day 16-20：补齐 Resources / Prompts / Roots / transport / contract test
```

## 常见误区

- 误区一：MCP Server 就是 Agent。
- 误区二：MCP Client 就是 LLM。
- 误区三：用了 MCP 就不用做权限校验。
- 误区四：工具列表写在 Prompt 里就等于 tool discovery。
- 误区五：stdio transport 可以随便 `System.out.println` 打日志。
- 误区六：Resources 和 Tools 没区别，所有读取都做成 Tool。

## 复盘提问

1. MCP Host、MCP Client、MCP Server 在本项目里分别对应什么？
2. 为什么 `tools/list` 比把工具描述写死在 Prompt 里更适合长期维护？
3. 为什么 Day 12 本地开发优先选 stdio，而不是一开始就做 Streamable HTTP？

## 验收标准

完成 Day 11 时，你应该能解释并验证：

- MCP Server、MCP Client、tool list、tool call、transport 的角色。
- `agent-app` 和 `mcp-server` 的职责边界。
- Agent 到 MCP Server 的调用链。
- 为什么 `search_code`、`git_history`、`read_config` 后续要迁移到 MCP。
- 为什么 MCP 不替代 Agent 编排，也不自动解决权限和安全问题。
