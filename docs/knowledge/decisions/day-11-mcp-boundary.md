# ADR：Day 11 MCP 角色与边界

## 背景

Day 08 定义了本地工具调用契约，Day 09-10 已实现三个本地只读工具：

- `search_code`
- `git_history`
- `read_config`

这些工具当前位于 `agent-app`，适合学习 Tool Calling 执行模型，但不适合作为长期架构边界。进入第 3 周后，需要把排障工具逐步迁移到 MCP Server，使 Agent 编排和工具执行解耦。

## 决策

Day 11 采用以下角色划分作为后续实现边界：

- `agent-app` 承担 MCP Host 角色，负责用户问题、Agent 编排、MCP Client 管理、observation 归档和诊断报告生成。
- `agent-app` 内部后续新增 MCP Client 组件，负责连接 `mcp-server`、执行 `initialize`、`tools/list`、`tools/call` 等协议交互。
- `mcp-server` 承担 MCP Server 角色，负责暴露只读 Tools、Resources、Prompts，并保护 Roots / allowedRoot 边界。
- Day 12 本地开发优先采用 stdio transport。
- Streamable HTTP 留到后续部署、远程访问或多客户端场景再评估。

## 备选方案

### 方案 A：继续把工具直接留在 Agent 内部

保持 `agent-app -> LocalTool` 调用方式。

### 方案 B：先用 stdio MCP Server 拆出协议边界

`agent-app` 通过 MCP Client 连接本地 `mcp-server` 子进程。

### 方案 C：一开始就做 Streamable HTTP MCP Server

把 `mcp-server` 做成独立 HTTP 服务，支持远程连接和多客户端。

## 取舍

选择方案 B。

原因：

- KISS：stdio 最适合本地学习和最小协议闭环。
- YAGNI：当前没有远程多客户端和生产鉴权需求，不需要先做 HTTP 服务。
- DRY：三个只读工具后续统一经 MCP Server 暴露，避免 Agent 内部长期维护一套私有工具协议。
- SOLID：Agent 编排依赖 MCP 协议边界，不依赖具体工具类；工具实现只负责单一只读能力。

方案 A 适合 Day 09-10 的学习阶段，但会让 Agent 和工具实现耦合。方案 C 更接近部署形态，但会过早引入认证、网络、安全头、会话管理和服务运维问题。

## 后果

正向后果：

- Agent 和工具服务边界更清晰。
- Day 12 可以用 `ping` 工具验证最小 MCP Server。
- Day 13-14 迁移真实工具时有明确目标。
- Day 15 的权限元数据和 Day 20 的 contract test 有协议承载点。

代价：

- 需要引入 MCP SDK 和协议生命周期概念。
- 本地开发要遵守 stdio 约束，不能向 stdout 打普通日志。
- `ToolResult` 与 MCP tool response 之间需要适配。
- 测试会从 Java 单元测试扩展到 MCP 协议级测试。

## 复查条件

出现以下情况时复查该决策：

- 需要远程部署 MCP Server。
- 需要多个 Agent 或客户端共享同一个 MCP Server。
- 需要标准认证、访问审计或多租户隔离。
- stdio 子进程启动、日志或生命周期管理成为瓶颈。
- MCP Java SDK 或 Spring AI MCP 的推荐 transport 发生明显变化。
