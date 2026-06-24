# Day 14：MCP 多只读工具注册

## 一句话定义

MCP 多只读工具注册是把 `search_code`、`git_history` 和 `read_config` 同时暴露为 MCP Server 的标准 Tools，让 Agent 通过统一协议发现、选择和调用不同排障能力。

## 解决的问题

- MCP Server 不再只有单个真实工具，而是形成排障 Agent 的最小只读工具集。
- `git_history` 和 `read_config` 从本地工具模式进入 MCP 协议边界。
- 每个工具都有独立名称、参数 schema、只读 annotation 和 JSON 文本返回格式。
- 单个工具的参数错误、权限拒绝或执行失败会以 `isError=true` 返回，不影响 Server 继续服务其他工具。

## 不解决的问题

- 不实现写操作工具。
- 不做 `riskLevel` 和 `readOnly` 业务元数据扩展；这是 Day 15 内容。
- 不实现 MCP Resources、Prompts、Roots。
- 不做自动工具选择和 Agent loop。
- 不把 `agent-app` 的本地工具类直接打包进 `mcp-server`。

## 在本项目中的位置

```text
agent-app
  MinimalMcpClient
    -> listTools()
    -> callTextTool("search_code", {"keyword": "..."})
    -> callTextTool("git_history", {"keyword": "...", "maxResults": 3})
    -> callTextTool("read_config", {"path": "config/application.yml"})

mcp-server
  MinimalMcpServerApplication
    -> SearchCodeMcpTool
    -> GitHistoryMcpTool
    -> ReadConfigMcpTool
    -> McpToolResults
    -> SensitiveValueRedactor
```

## 最小代码证据

- `MinimalMcpServerApplication` 注册 `ping`、`search_code`、`git_history` 和 `read_config`。
- `GitHistoryMcpTool` 暴露 `keyword`、`maxResults`，只查询允许根目录自身的 Git 仓库。
- `ReadConfigMcpTool` 暴露 `path`，只允许读取根目录内的常见配置文件并脱敏。
- `McpToolResults` 统一 `status / summary / evidence / errorCode / errorMessage` JSON 文本格式。
- `MinimalMcpServerApplicationTest` 验证工具列表、schema、annotation、成功返回和错误隔离。
- `MinimalMcpClientTest` 通过真实 stdio MCP Server 调用三个只读工具。

## 常见误区

- 只在 Server 里注册工具，不用 `tools/list` 验证 schema。
- 多个工具共用一段大 handler，导致错误边界和职责不清。
- 把根目录作为工具调用参数交给模型决定，削弱访问边界。
- `read_config` 只检查字符串路径，不检查真实路径和符号链接。
- `git_history` 从子目录向上查找父级 Git 仓库，越过允许根目录。
- MCP 工具失败时抛异常到协议层，而不是返回可操作的 `isError=true` 结果。

## 自测问题

1. `git_history` 为什么要求 `.git` 位于允许根目录自身，而不是从子目录向上查找？
2. `read_config` 为什么要同时拒绝绝对路径、路径穿越和符号链接？
3. 当一个 MCP 工具参数错误时，为什么要返回 `isError=true` 的结构化错误，而不是让 Server 进程失败？

## 今日结论

Day 14 的核心是从“一个 MCP 工具可用”推进到“多个只读工具作为稳定工具集可发现、可调用、可验证”。Agent 仍只依赖 MCP 协议契约，具体 Git 查询、配置读取、脱敏和错误隔离都留在 MCP Server 内部。
