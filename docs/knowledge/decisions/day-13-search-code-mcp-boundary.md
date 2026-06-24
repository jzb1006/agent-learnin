# ADR：Day 13 search_code MCP 边界

## 背景

Day 09 已在 `agent-app` 中实现本地只读工具 `LocalCodeSearchTool`。Day 12 已搭建最小 MCP Server 并注册 `ping`。

Day 13 需要把第一个真实排障工具迁移到 MCP。关键问题不是搜索算法，而是确定 Agent、MCP Client、MCP Server 和工具执行逻辑之间的边界。

## 决策

将 `search_code` 作为 MCP Tool 注册到 `mcp-server`：

```text
search_code(keyword)
```

具体决策：

- `search_code` 的 MCP schema 只暴露 `keyword`。
- 允许搜索根目录由 Server 环境变量 `MCP_SEARCH_CODE_ROOT` 配置。
- Server 未设置环境变量时默认使用当前工作目录。
- MCP Server 自己实现路径校验、符号链接防护、Java 文件过滤和结果裁剪。
- 工具结果通过 MCP text content 返回 JSON。
- `agent-app` 只通过 `MinimalMcpClient` 发现和调用工具。
- Day 13 不抽共享工具模块，避免过早设计。

## 备选方案

### 方案 A：让 MCP Server 直接依赖 agent-app 的 LocalCodeSearchTool

优点是复用现有代码。

缺点是模块方向错误：工具服务反向依赖 Agent 应用层，破坏职责边界。

### 方案 B：抽一个 shared-tool-core 模块复用搜索逻辑

优点是减少重复。

缺点是 Day 13 只迁移一个工具，提前抽模块会引入 Maven 多模块结构和依赖治理，超过当天目标。

### 方案 C：在 mcp-server 内实现 MCP 版 search_code

优点是边界清晰，`mcp-server` 自己拥有工具执行能力，Agent 只依赖 MCP。

缺点是和 Day 09 本地工具存在少量重复逻辑，后续工具数量增加时再评估共享模块。

## 取舍

选择方案 C。

原因：

- KISS：最小改动完成真实 MCP 工具迁移。
- YAGNI：当前只有一个工具迁移，不提前抽共享库。
- DRY：调用协议和 Client 复用 `MinimalMcpClient`，搜索实现重复保持可控。
- SOLID：`agent-app` 负责编排和 MCP 调用，`mcp-server` 负责工具执行和边界保护。

## 后果

正向后果：

- Agent 侧通过 MCP 协议调用真实只读工具。
- `search_code` 的参数 schema、annotations 和错误语义可被测试验证。
- 允许根目录不暴露给模型，降低越权风险。
- Day 14 可以沿用同一模式迁移 `git_history` 和 `read_config`。

代价：

- `mcp-server` 中出现与 Day 09 本地工具相似的搜索逻辑。
- Agent 侧集成测试依赖最新 `mcp-server` shaded JAR，需要先执行 package。
- 当前工具结果仍以 JSON 文本返回，Agent 侧尚未抽统一 MCP ToolResult parser。

## 复查条件

出现以下情况时复查该决策：

- `git_history` 和 `read_config` 迁移后重复逻辑明显增加。
- Day 20 contract test 需要统一 MCP 工具结果 schema。
- Agent 编排层需要把 MCP JSON 文本稳定解析成 Java 对象。
- 需要支持多个目标项目 root 或 MCP Roots。
- 需要把 MCP Server 部署为远程服务。
