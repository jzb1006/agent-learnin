# ADR：Day 07 结构化输出边界

## 背景

Day 06 已实现最小 `ChatModelClient`，只能返回普通文本。

Day 07 需要让排障 Agent 输出稳定解析为 Java 对象，字段包含 `summary / evidence / nextActions / riskLevel`。同时，Day 08 以后才进入 Tool Calling，本节点不能提前实现工具调用或 MCP 能力。

## 决策

在 `agent-app` 中新增独立 `report` 包：

- `DiagnosticReport`：诊断报告强类型契约。
- `RiskLevel`：风险等级枚举。
- `DiagnosticReportParser`：JSON 解析和字段校验。
- `DiagnosticReportGenerator`：组合 `ChatModelClient` 和 parser，并在解析失败时受控重试。

`OpenAiCompatibleChatModelClient` 继续只负责 OpenAI-compatible HTTP 调用。

## 备选方案

### 方案 A：独立 `report` 包

保持模型调用、报告生成和报告解析分离。

### 方案 B：直接修改 `OpenAiCompatibleChatModelClient`

让模型客户端同时负责 HTTP、prompt、报告解析和重试。

### 方案 C：直接引入 Spring AI 结构化输出

使用框架能力生成强类型对象。

## 取舍

选择方案 A。

原因：

- KISS：只处理当前最小报告契约。
- YAGNI：不提前引入 Spring AI 配置和复杂输出转换器。
- DRY：报告字段集中在 `DiagnosticReport`。
- SOLID：模型客户端不承担报告语义，报告解析器不承担模型调用。

方案 B 会让模型客户端职责过重。方案 C 更接近后续主线，但会干扰 Day 07 对 JSON schema、解析、字段校验和失败重试的学习目标。

## 后果

正向后果：

- 单元测试不依赖真实模型。
- 结构化输出失败可以被明确识别。
- 后续接入 Tool Calling、MCP、trace 和 eval 时可复用报告契约。

代价：

- 当前 schema 通过 prompt 约束，不是模型 API 原生强约束。
- 后续接 Spring AI 时可能需要增加一层适配，但不影响报告契约。

## 复查条件

出现以下情况时复查该决策：

- 项目切换为 Spring AI 原生结构化输出。
- 诊断报告字段需要区分事实证据、模型推断、引用来源和工具 trace。
- Agent 编排进入 Day 31-35，需要最终报告和中间观察状态分离。
