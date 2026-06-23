# Day 08：Tool Calling 执行模型

## 一句话定义

Tool Calling 是模型根据工具 schema 选择外部工具并生成结构化参数，由程序校验、执行并返回结构化结果的机制。

## 解决的问题

- 让排障 Agent 能主动获取真实证据，而不是只靠模型猜测。
- 把工具选择、参数、执行结果和失败状态变成可测试契约。
- 为 Day 09-10 的 `search_code / git_history / read_config` 提供统一接口。
- 为后续 MCP 工具迁移准备本地 Java 抽象。

## 不解决的问题

- 不实现具体工具逻辑。
- 不做 MCP 协议接入。
- 不做多轮 Agent Loop。
- 不保证工具结果足够得出最终结论。
- 不允许任何写操作或高风险操作默认执行。

## 在本项目中的位置

```text
agent-app
  llm.ChatModelClient
  report.DiagnosticReport
  tool.TroubleshootingTool
    -> ToolDefinition
    -> ToolCall
    -> ToolResult
    -> ToolEvidence
```

后续 `search_code`、`git_history`、`read_config` 都应该实现 `TroubleshootingTool`。

## 最小代码证据

- `ToolDefinition` 描述工具名、描述、参数、只读和幂等。
- `ToolParameter` 描述参数名、类型、说明和必填性。
- `ToolCall` 表示一次模型生成的工具调用。
- `ToolResult` 区分成功证据和失败语义。
- `ToolEvidence` 要求证据必须带来源。
- `ToolContractTest` 覆盖 schema、重复参数、参数复制、成功/失败结果和统一接口。

## 常见误区

- 把 Tool Calling 当成模型直接访问文件或执行命令。
- 只让工具返回文本，不返回状态和来源。
- 参数来自模型就不做校验。
- 只读工具不设置访问边界。
- 在工具描述里写复杂业务推理，而不是让工具专注返回事实。

## 自测问题

1. Tool Calling 和结构化输出的核心区别是什么？
2. 程序为什么不能直接信任模型生成的工具参数？
3. 工具失败时为什么不能把错误文本当证据交给最终报告？

## 今日结论

Day 08 的核心不是实现一个工具，而是先建立工具调用契约：模型可以请求工具，但程序必须掌握校验、权限、执行和失败隔离。这个边界清楚后，Day 09-10 才能安全地实现本地只读工具。
