# ADR：Day 08 工具调用契约边界

## 背景

Day 06 已建立 `ChatModelClient`，Day 07 已建立 `DiagnosticReport` 结构化输出契约。

Day 08 开始进入 Tool Calling。后续 Day 09-10 会实现 `search_code`、`git_history`、`read_config` 三个本地只读工具，再在 Day 11-15 迁移到 MCP。

本节点需要先明确工具调用的执行模型和统一接口，避免每个工具各自返回不同格式。

## 决策

在 `agent-app` 中新增 `tool` 包，定义最小只读工具契约：

- `TroubleshootingTool`：排障工具统一接口。
- `ToolDefinition`：工具 schema、只读属性和幂等性。
- `ToolParameter` / `ToolParameterType`：工具参数定义。
- `ToolCall`：模型生成的工具调用请求。
- `ToolResult` / `ToolResultStatus`：工具成功证据和失败语义。
- `ToolEvidence`：带来源的证据片段。

本节点不实现任何真实工具逻辑。

## 备选方案

### 方案 A：先定义本地 Java 工具契约

用最小 Java record / enum / interface 描述工具调用模型。

### 方案 B：直接实现 `search_code`

先写代码搜索功能，后续再抽象统一接口。

### 方案 C：直接接入 MCP Tool

跳过本地契约，直接用 MCP Server 暴露工具。

## 取舍

选择方案 A。

原因：

- KISS：只解决 Day 08 的概念和接口边界。
- YAGNI：不提前做工具注册中心、MCP adapter 或复杂 JSON Schema 生成。
- DRY：所有工具统一返回 `ToolResult`。
- SOLID：Agent 编排层依赖 `TroubleshootingTool` 抽象，具体工具实现可以替换为本地实现或 MCP 实现。

方案 B 容易让 `search_code` 的实现细节先污染接口设计。方案 C 会把 MCP transport、协议和工具语义混在一起，不利于先理解 Tool Calling。

## 后果

正向后果：

- Day 09-10 可以直接复用同一工具契约。
- 工具成功和失败语义可测试。
- 证据来源成为强制字段，降低模型推断混入事实的风险。
- 后续迁移 MCP 时有清晰的本地语义模型。

代价：

- 当前 `ToolParameterType` 只覆盖 `STRING / INTEGER / BOOLEAN`，复杂对象和数组暂不支持。
- 当前 `ToolCall.arguments` 使用 `Map<String, String>`，后续如果工具需要复杂参数，需要引入更强的值类型或 JSON 节点。
- 当前没有工具注册和参数自动校验器，Day 09-10 仍需在具体工具中做边界校验。

## 复查条件

出现以下情况时复查该决策：

- 真实工具需要数组、对象或枚举参数。
- Agent 需要动态列出多个工具并按名称路由执行。
- 迁移 MCP 时需要从本地 `ToolDefinition` 生成 MCP tool schema。
- 引入 Spring AI 原生 Tool Calling 后需要适配框架工具定义。
