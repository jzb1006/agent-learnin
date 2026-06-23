# Day 08：理解 Tool Calling 的执行模型

## 今日节点

Day 08：理解 Tool Calling 的执行模型。

## 今日主题

让模型不只是“回答问题”，而是在需要证据时选择一个工具、生成参数、接收工具结果，再基于结果继续生成诊断报告。

## 今天解决的问题

- Tool Calling 的调用链路是什么。
- 工具 schema 应该描述哪些信息。
- 工具参数和普通 Prompt 文本有什么区别。
- 工具返回值为什么要区分成功证据和失败语义。
- 只读工具为什么默认要求幂等。
- 本项目 Day 09-10 的只读工具应该遵守什么统一接口。

## 今天不解决的问题

- 不实现真实 `search_code` 搜索逻辑，放到 Day 09。
- 不实现 `git_history` 和 `read_config`，放到 Day 10。
- 不接入 MCP Client / Server，放到 Day 11 以后。
- 不做多轮 Agent Loop、trace 持久化、RAG 或 Memory。
- 不做任何重启、部署、配置修改、数据库写入等高风险操作。

## 一句话定义

Tool Calling 是模型根据工具 schema 选择一个外部工具，并生成结构化参数；程序执行工具后，把结构化结果再交还给模型或 Agent 编排层。

## 概念讲解

### Tool Calling 解决什么

普通 LLM 只能基于上下文生成回答。排障场景里，结论必须依赖证据，例如：

- 代码里是否真的存在某个方法。
- 配置文件是否真的包含某个开关。
- Git 历史里是否真的有相关提交。

Tool Calling 让模型把“我需要查证据”表达为一个可执行调用：

```json
{
  "toolName": "search_code",
  "arguments": {
    "keyword": "HikariPool"
  }
}
```

程序再执行工具，并返回统一结果：

```json
{
  "status": "SUCCESS",
  "summary": "找到 1 个匹配片段",
  "evidence": [
    {
      "source": "src/main/java/App.java:12",
      "content": "HikariPool timeout"
    }
  ]
}
```

### Tool Calling 不解决什么

Tool Calling 不等于工具一定会成功，也不等于工具结果一定足够回答问题。

常见失败包括：

- 参数缺失或格式错误。
- 请求越过允许根目录。
- 工具执行超时。
- 目标文件不存在。
- 搜索结果太多，需要裁剪。
- 工具返回空结果，只能说明“在当前范围内未找到”，不能证明事实不存在。

所以工具返回值必须显式表达失败语义，而不是把失败包装成一段看似正常的文本。

### 和 Java 后端开发的类比

Tool Calling 类似 Controller 调 Service：

```text
Controller 入参 DTO
  -> Service 方法调用
  -> ServiceResult / Exception
  -> Controller 统一响应
```

区别是：

- Controller 调用由人类或前端请求触发。
- Tool Calling 的工具选择和参数通常由模型生成。
- 因为参数来自模型，更需要 schema、校验、权限边界和失败状态。

## 执行模型

```text
用户问题
  -> 模型读取工具列表和 schema
  -> 模型选择工具并生成参数
  -> Agent 校验工具名、参数和权限
  -> 程序执行工具
  -> 工具返回 ToolResult
  -> Agent 把结果作为 observation
  -> 模型继续回答或请求下一个工具
```

关键点：

- 模型负责“选择工具和生成参数”。
- 程序负责“校验、权限、执行和失败隔离”。
- 工具负责“只做一个明确能力，并返回结构化结果”。
- 最终报告负责“引用证据并给出诊断结论”。

## 普通文本、结构化输出、Tool Calling 的区别

| 类型 | 主要目的 | 程序能否稳定消费 | 是否执行外部能力 | 本项目例子 |
| --- | --- | --- | --- | --- |
| 普通文本回答 | 给人阅读 | 弱 | 否 | Day 06 `ChatModelResponse.content` |
| 结构化输出 | 让模型按固定字段输出对象 | 中到强，取决于校验 | 否 | Day 07 `DiagnosticReport` |
| Tool Calling | 让模型请求程序执行工具 | 强，基于 schema 和结果契约 | 是 | Day 08 `ToolCall` / `ToolResult` |

容易混淆的一点：

- 结构化输出是“模型输出最终对象”。
- Tool Calling 是“模型请求一次外部动作”。

排障 Agent 最终会同时使用两者：先调用工具拿证据，再生成结构化诊断报告。

## 项目映射

代码落点：

```text
projects/mcp-troubleshooting-agent/agent-app/
  src/main/java/io/github/jiangzhibin/agentlearning/tool/
    TroubleshootingTool.java
    ToolDefinition.java
    ToolParameter.java
    ToolParameterType.java
    ToolCall.java
    ToolResult.java
    ToolResultStatus.java
    ToolEvidence.java
  src/test/java/io/github/jiangzhibin/agentlearning/tool/
    ToolContractTest.java
```

职责划分：

```text
ToolDefinition
  描述工具 schema、只读属性、幂等性。

ToolParameter
  描述参数名称、类型、说明和是否必填。

ToolCall
  表示模型选择工具后生成的工具名和参数。

ToolResult
  表示工具执行后的成功证据或失败语义。

ToolEvidence
  表示带来源的证据片段，避免把模型推断当事实。

TroubleshootingTool
  定义排障工具统一接口，后续 search_code / git_history / read_config 都实现它。
```

## 只读工具接口规范

Day 08 设计的只读工具必须遵守：

| 规则 | 说明 |
| --- | --- |
| 单一职责 | 一个工具只做一个明确查询，例如 `search_code` 只搜索源码 |
| schema 明确 | 工具名、描述、参数名、参数类型、必填字段都必须可枚举 |
| 默认只读 | `readOnly=true`，不修改文件、配置、数据库或运行时状态 |
| 默认幂等 | 同样输入重复执行，不改变系统状态 |
| 参数先校验 | 空参数、未知参数、越界路径必须返回受控失败 |
| 结果带来源 | 成功结果必须包含 `ToolEvidence.source` |
| 失败不伪装 | 失败返回 `INVALID_ARGUMENTS / PERMISSION_DENIED / EXECUTION_FAILED` |
| 不泄露敏感值 | 后续 `read_config` 必须脱敏 token、secret、password |

## 失败语义

当前 `ToolResultStatus` 包含：

| 状态 | 含义 | 是否可作为证据 |
| --- | --- | --- |
| `SUCCESS` | 工具成功执行并返回证据 | 是 |
| `INVALID_ARGUMENTS` | 参数缺失、为空或格式不合法 | 否 |
| `PERMISSION_DENIED` | 访问越过允许边界或工具不允许执行 | 否 |
| `EXECUTION_FAILED` | 工具内部执行失败，例如 IO 错误 | 否 |

设计重点：

- 成功结果必须包含证据。
- 失败结果必须包含错误码和错误信息。
- Agent 不应该把失败文本塞进最终证据列表。

## 今日决策

采用“先定义本地 Java 工具契约，再实现具体工具”的方案。

原因：

- KISS：只引入 Day 09-10 需要的最小接口。
- YAGNI：不提前实现工具注册中心、MCP 适配器或复杂 JSON Schema 生成。
- DRY：所有工具统一返回 `ToolResult`，避免每个工具各写一套成功和失败格式。
- SOLID：Agent 编排层依赖 `TroubleshootingTool` 抽象，不依赖具体 `search_code` 实现。

## TDD 过程证据

红灯：先写 `ToolContractTest`。

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=ToolContractTest

失败原因：
找不到符号 ToolDefinition / ToolParameter / ToolCall / ToolResult / TroubleshootingTool
```

绿灯：补最小工具契约实现。

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=ToolContractTest

Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 和后续 Day 的关系

```text
Day 06：模型返回普通文本
Day 07：模型输出稳定解析成 DiagnosticReport
Day 08：定义工具调用执行模型和只读工具契约
Day 09：实现 search_code，返回 ToolResult
Day 10：实现 git_history / read_config，复用同一 ToolResult
Day 11-15：把本地工具迁移为 MCP Tool
```

## 常见误区

- 误区一：把 Tool Calling 当成“模型自己执行代码”。
- 误区二：工具返回一段普通文本，不区分证据和错误。
- 误区三：工具参数来自模型，就跳过服务端校验。
- 误区四：只读工具没有风险，不需要权限边界。
- 误区五：把业务判断写死在工具描述里，而不是让工具只提供事实证据。

## 验收标准

完成 Day 08 时，你应该能解释并验证：

- Tool Calling、结构化输出和普通文本回答的区别。
- 工具 schema 至少需要包含工具名、描述、参数和权限元数据。
- 为什么只读工具也要校验参数和访问边界。
- 为什么工具成功结果必须带来源。
- 为什么失败结果不能伪装成普通证据。

## 复习问题

Day 08 直接关系到 Agent 核心执行模型，建议回答：

1. Tool Calling 中，模型负责什么，程序负责什么？
2. 为什么 `ToolResult` 要区分 `SUCCESS`、`INVALID_ARGUMENTS`、`PERMISSION_DENIED` 和 `EXECUTION_FAILED`？
3. 为什么只读工具仍然需要 `readOnly`、`idempotent` 和来源证据？
