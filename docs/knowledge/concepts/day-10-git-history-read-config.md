# Day 10：git_history 与 read_config 本地只读工具

## 一句话定义

`git_history` 和 `read_config` 是两个本地只读排障工具，分别把 Git 提交历史和配置文件内容转换成可追溯、已脱敏的 `ToolResult` 证据。

## 解决的问题

- 让排障 Agent 能查询“最近改了什么”。
- 让排障 Agent 能读取配置证据，而不是让模型猜配置。
- 在工具层统一脱敏 `token`、`secret`、`password`、`api-key` 等敏感值。
- 继续复用 Day 08 的 `TroubleshootingTool`、`ToolCall`、`ToolResult` 和 `ToolEvidence` 契约。

## 不解决的问题

- 不做 Git diff 分析。
- 不做配置语义解释或根因判断。
- 不读取允许根目录外的仓库或配置。
- 不支持数据库配置中心、远程配置中心或生产 API。
- 不接 MCP；Day 14 再迁移到 MCP Server。

## 在本项目中的位置

```text
agent-app
  tool.TroubleshootingTool
    -> LocalGitHistoryTool
       -> ToolCall(keyword?, maxResults?)
       -> ToolResult
    -> LocalConfigReadTool
       -> ToolCall(path)
       -> ToolResult
    -> SensitiveValueRedactor
       -> token / secret / password / api-key 脱敏
```

这两个工具仍是本地 Java 实现。后续 MCP 阶段会把工具能力迁移或适配到 `mcp-server`。

## 最小代码证据

- `LocalGitHistoryTool` 实现 `TroubleshootingTool`。
- `git_history` 只查询允许根目录自身的 Git 仓库，不向上寻找外部仓库。
- `git_history` 支持 `keyword` 和 `maxResults`，最多返回 10 条提交证据。
- `LocalConfigReadTool` 实现 `TroubleshootingTool`。
- `read_config` 只读取允许根目录内的 `.properties`、`.yml`、`.yaml`、`.env`、`.conf`。
- `read_config` 拒绝路径穿越、符号链接和未知参数。
- `SensitiveValueRedactor` 统一处理敏感值脱敏。
- 单元测试覆盖正常读取、脱敏、路径边界、符号链接和参数错误。

## 常见误区

- 把 Git 历史查询做成根因判断。
- 只脱敏配置文件，不脱敏提交信息。
- 允许从子目录向上读取父级 Git 仓库，导致越过工具允许根目录。
- 把“没有匹配提交”当成没有发生过相关改动。
- 只读工具省略路径校验和敏感信息处理。

## 自测问题

1. `git_history` 为什么不允许从允许根目录的子目录向上读取父级 Git 仓库？
2. `read_config` 为什么要拒绝符号链接？
3. 为什么敏感值脱敏应该放在工具层，而不是等模型回答时再处理？

## 今日结论

Day 10 的重点不是“能不能读 Git 和配置”，而是把只读证据读取做成安全、可测试、可追溯的工具能力：路径受限、参数受控、敏感值先脱敏、失败语义统一。
