# Day 13：search_code 接入 MCP

## 一句话定义

`search_code` 接入 MCP 是把代码搜索能力从 Agent 内部本地工具迁移到 MCP Server 暴露的标准 Tool，Agent 通过 MCP Client 发现和调用它。

## 解决的问题

- Agent 不再直接依赖 `LocalCodeSearchTool` 的具体实现。
- 代码搜索能力进入 MCP Server 的独立服务边界。
- 工具 schema、参数校验、返回格式和只读属性可以通过 MCP 验证。
- 后续 `git_history`、`read_config` 可以沿用同一迁移模式。

## 不解决的问题

- 不迁移 `git_history` 和 `read_config`。
- 不做 MCP Resources、Prompts、Roots。
- 不做远程 transport。
- 不做 Agent 自动规划和多轮工具调用。
- 不做写操作或高风险审批。

## 在本项目中的位置

```text
agent-app
  MinimalMcpClient
    -> listTools()
    -> callTextTool("search_code", {"keyword": "..."})

mcp-server
  MinimalMcpServerApplication
    -> SearchCodeMcpTool
       -> allowedRoot from MCP_SEARCH_CODE_ROOT
       -> Java file search
       -> JSON text result
```

## 最小代码证据

- `SearchCodeMcpTool` 注册 MCP Tool `search_code`。
- `search_code` input schema 只允许 `keyword`。
- `SearchCodeMcpTool` 校验 allowed root、关键词、未知参数和符号链接。
- `MinimalMcpServerApplication` 同时注册 `ping` 和 `search_code`。
- `MinimalMcpClient` 支持环境变量和带参数工具调用。
- `MinimalMcpClientTest` 通过真实 stdio 子进程调用 `search_code`。

## 常见误区

- 把 MCP Tool 当成简单方法导出，忽略 schema 和权限边界。
- 让模型传入 `root`，把访问边界交给模型输出。
- 只跑 Server 单测，不重新打包 JAR 就跑 Agent 集成测试。
- 把 MCP content 当成 Java 对象，忽略协议返回格式。
- 认为用了 MCP 就自动防越权，实际 Server 仍要做路径校验。

## 自测问题

1. 为什么 `root` 应该是 MCP Server 的运行配置，而不是 `search_code` 的调用参数？
2. `tools/list` 验证了什么，`tools/call search_code` 又验证了什么？
3. `search_code` 接入 MCP 后，Agent 侧还应该知道搜索是用 `Files.walk` 实现的吗？

## 今日结论

Day 13 的核心不是重写搜索算法，而是建立真实工具的 MCP 边界：Agent 只依赖协议契约，MCP Server 负责工具执行、安全边界和结果序列化。
