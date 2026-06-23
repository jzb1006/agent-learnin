# Day 09：search_code 本地只读工具

## 一句话定义

`search_code` 是一个本地只读代码搜索工具，用关键词扫描允许根目录内的 Java 源码，并返回带来源的匹配片段。

## 解决的问题

- 让排障 Agent 能基于代码证据回答问题。
- 把模型生成的搜索意图变成受控的本地文件读取。
- 限制搜索输入、访问范围和结果数量。
- 为后续 MCP Tool 迁移准备可测试的本地实现。

## 不解决的问题

- 不做语义检索、RAG、Embedding 或 rerank。
- 不支持复杂查询语法。
- 不搜索配置、日志或 Git 历史。
- 不判断根因。
- 不执行任何写操作。

## 在本项目中的位置

```text
agent-app
  tool.TroubleshootingTool
    -> LocalCodeSearchTool
       -> ToolCall(keyword)
       -> ToolResult
       -> ToolEvidence(source, content)
```

`search_code` 当前仍在 `agent-app` 中作为本地实现存在。Day 13 会把它迁移或适配到 MCP Tool。

## 最小代码证据

- `LocalCodeSearchTool` 实现 `TroubleshootingTool`。
- `definition()` 暴露只读、幂等的 `search_code` schema。
- `execute()` 校验工具名、`keyword`、未知参数和关键词长度。
- 文件遍历只接受 `.java` 普通文件。
- 符号链接会被跳过。
- 每条证据使用 `relative/path.java:line` 作为来源。
- `LocalCodeSearchToolTest` 覆盖正常搜索、参数错误、结果裁剪和越界符号链接。

## 常见误区

- 忽略符号链接导致允许根目录被绕过。
- 把所有匹配结果都返回给模型。
- 把没有匹配当成绝对事实。
- 把搜索工具做成根因判断工具。
- 因为工具只读就省略参数校验。

## 自测问题

1. `search_code` 为什么只能扫描允许根目录？
2. `ToolEvidence.source` 为什么要包含文件路径和行号？
3. 搜索结果为什么最多返回 5 条？

## 今日结论

Day 09 的核心不是“写一个 grep”，而是把代码搜索做成 Agent 可安全调用的工具：参数受控、路径受限、结果可裁剪、证据可追溯、失败语义清晰。
