# Day 11：MCP 角色

## 一句话定义

MCP 是 Agent 应用和外部上下文能力之间的标准协议；MCP Host 通过 MCP Client 连接 MCP Server，发现并调用 Server 暴露的 Tools、Resources 和 Prompts。

## 解决的问题

- 把排障工具从 Agent 编排层拆到独立协议边界。
- 让工具能力可以被发现、调用、测试和复用。
- 统一本地工具和后续远程工具的连接模型。
- 为权限分类、contract test、Resources、Prompts 和 Roots 打基础。

## 不解决的问题

- 不替代 LLM。
- 不替代 Agent 的计划、观察、反思和报告生成。
- 不自动保证工具安全。
- 不自动做 RAG、Memory 或上下文压缩。
- 不决定最终诊断结论。

## 在本项目中的位置

```text
agent-app
  MCP Host
  MCP Client
  Agent 编排
  诊断报告生成

mcp-server
  MCP Server
  search_code
  git_history
  read_config
  Resources
  Prompts
  Roots
```

Day 11 只是建立边界模型。Day 12 才开始搭最小 MCP Server。

## 最小证据

- Day 08-10 的本地工具当前仍在 `agent-app/tool`。
- Day 11 的调用链图明确了后续目标：`agent-app -> MCP Client -> mcp-server -> read-only tools`。
- `mcp-server/README.md` 已定义该模块负责 Tools、Resources、Prompts 和 Roots。
- Day 12 将优先用 stdio transport 暴露 `ping` 工具。

## 常见误区

- 把 MCP Server 当成 Agent 本身。
- 把 MCP Client 当成模型。
- 认为 MCP 等于只做 tool calling。
- 认为工具接入 MCP 后就不需要输入校验和权限边界。
- 在 stdio MCP Server 的 stdout 写普通日志，破坏协议消息。
- 把所有上下文读取都建模成 Tool，忽略 Resources。

## 自测问题

1. MCP Host、MCP Client、MCP Server 的职责分别是什么？
2. `tools/list` 和 `tools/call` 分别解决什么问题？
3. 为什么本项目 Day 12 优先选择 stdio transport？

## 今日结论

Day 11 的核心不是写代码，而是建立协议边界：Agent 负责编排和判断，MCP Client 负责连接和协议交互，MCP Server 负责暴露只读工具和保护执行边界。
