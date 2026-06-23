# Day 07：结构化输出

## 一句话定义

结构化输出就是让模型按明确的 JSON schema 返回内容，再由程序解析、校验并转换成强类型对象。

## 解决的问题

- 让模型回复能被程序稳定消费，而不是只给人阅读。
- 固定排障报告的字段：`summary / evidence / nextActions / riskLevel`。
- 让解析失败、缺字段和非法枚举变成可识别的受控失败。
- 为后续 Tool Calling、MCP、trace 和 eval 提供稳定报告契约。

## 不解决的问题

- 不证明模型结论真实。
- 不执行工具调用。
- 不提供证据来源引用。
- 不做 Agent 多轮编排。
- 不替代后续 RAG、Memory 或 eval。

## 在本项目中的位置

```text
agent-app
  llm.ChatModelClient
    -> report.DiagnosticReportGenerator
    -> report.DiagnosticReportParser
    -> report.DiagnosticReport
```

`DiagnosticReport` 是排障 Agent 当前最小诊断报告契约。

## 最小代码证据

- `DiagnosticReport` 校验 `summary / evidence / nextActions / riskLevel`。
- `DiagnosticReportParser` 把 JSON 解析为 `DiagnosticReport`，失败时抛出 `DiagnosticReportParseException`。
- `DiagnosticReportGenerator` 通过 prompt 写入 JSON schema，解析失败时最多重试。
- `DiagnosticReportParserTest` 覆盖成功解析、缺字段和非法风险等级。
- `DiagnosticReportGeneratorTest` 覆盖 schema 约束和解析失败重试。

## 常见误区

- 把结构化输出等同于 Tool Calling。
- 只校验 JSON 语法，不校验字段语义。
- 把模型生成的 `evidence` 当成真实证据。
- 解析失败后返回空对象，导致后续 Agent 状态污染。
- 在报告生成器里直接实现工具能力，破坏职责边界。

## 自测问题

1. 结构化输出和普通文本回答的核心区别是什么？
2. 结构化输出为什么仍然可能产生幻觉？
3. 为什么解析失败要让上层感知，而不是吞掉异常？

## 今日结论

Day 07 的核心不是“让模型写得像 JSON”，而是建立可校验、可失败、可测试的输出契约。后续 Agent 可以基于这个契约继续接工具、MCP、RAG 和 trace。
