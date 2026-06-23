# Day 07：实现结构化输出

## 今日节点

Day 07：实现结构化输出。

## 今日主题

把模型生成的诊断报告从普通文本约束为稳定的 Java 对象：`summary / evidence / nextActions / riskLevel`。

## 今天解决的问题

- 为什么模型输出不能直接当可信业务对象使用。
- 诊断报告的 JSON schema 应该包含哪些字段。
- 如何把 JSON 解析为 Java 类型。
- 如何校验必填字段、数组字段和风险等级。
- 模型第一次输出不合规时，如何做一次受控重试。

## 今天不解决的问题

- 不实现 Tool Calling，工具 schema 和工具执行放到 Day 08 以后。
- 不实现本地只读工具 `search_code`、`git_history`、`read_config`。
- 不接入 MCP Client / Server。
- 不实现 Agent Loop、trace 持久化、RAG、Memory 或 CLI / REST 入口。
- 不把解析失败伪装成成功报告。

## 一句话定义

结构化输出就是让模型按明确的 JSON schema 返回内容，再由程序解析、校验并转换成强类型对象。

## 概念讲解

### 结构化输出解决什么

普通文本适合给人看，但程序很难稳定消费。

排障 Agent 后续需要判断报告里有哪些证据、下一步要做哪些只读动作、风险等级是多少。如果报告只是自然语言段落，程序无法可靠区分事实、建议和风险。

结构化输出把报告变成固定字段：

```json
{
  "summary": "简短诊断摘要",
  "evidence": ["证据 1", "证据 2"],
  "nextActions": ["只读后续动作 1"],
  "riskLevel": "LOW | MEDIUM | HIGH"
}
```

### 结构化输出不解决什么

结构化输出只保证形状更稳定，不保证内容一定正确。

例如模型可以输出合法 JSON：

```json
{
  "summary": "数据库连接池耗尽",
  "evidence": ["日志出现连接池超时"],
  "nextActions": ["只读查看错误日志"],
  "riskLevel": "MEDIUM"
}
```

但如果 `evidence` 并非来自真实日志，而是模型猜测，它仍然只是推断。后续 RAG、工具调用和 trace 会负责证据来源，不是 Day 07 解决。

### 和 Java 后端开发的类比

结构化输出类似 Controller 接收请求 DTO：

- JSON 语法合法，只代表能反序列化。
- 字段存在，只代表满足基本契约。
- 字段语义是否真实，还需要 Service 层和数据来源验证。

所以 Day 07 做的是“DTO + Validation”，不是完整业务判断。

## 项目映射

```text
用户问题
  -> DiagnosticReportGenerator.generate(question)
  -> ChatModelClient.complete(prompt)
  -> 模型输出 JSON 文本
  -> DiagnosticReportParser.parse(json)
  -> DiagnosticReport
```

代码落点：

```text
projects/mcp-troubleshooting-agent/agent-app/
  src/main/java/io/github/jiangzhibin/agentlearning/report/
    DiagnosticReport.java
    DiagnosticReportGenerator.java
    DiagnosticReportParser.java
    DiagnosticReportParseException.java
    RiskLevel.java
  src/test/java/io/github/jiangzhibin/agentlearning/report/
    DiagnosticReportGeneratorTest.java
    DiagnosticReportParserTest.java
```

## 设计方案

### 方案 A：独立 report 包解析 JSON

优点：

- 职责清晰，结构化输出和底层模型 HTTP 客户端解耦。
- 可以用 fake `ChatModelClient` 测试，不依赖真实 API。
- 后续接 Tool Calling、MCP 或 Spring AI 时，报告契约可复用。

缺点：

- 当前 JSON schema 只是 prompt 约束，不是模型 API 原生强约束。

### 方案 B：直接改 `OpenAiCompatibleChatModelClient`

优点：

- 调用链更短。

缺点：

- 模型客户端会同时承担 HTTP 调用、Prompt 约束、报告解析和重试，职责过重。
- 后续普通文本调用和诊断报告调用不好复用。

### 方案 C：直接引入 Spring AI 结构化输出

优点：

- 更接近后续 Spring AI 主线。

缺点：

- Day 07 会被框架配置稀释，不利于先理解 JSON schema、解析和校验边界。

## 今日决策

采用方案 A：新增独立 `report` 包。

原因：

- KISS：只实现当前诊断报告的四个字段。
- YAGNI：不提前引入 Spring AI、Bean 配置或复杂重试策略。
- DRY：诊断报告结构集中在 `DiagnosticReport`，不散落在 Prompt 和测试里。
- SOLID：模型调用、报告生成、报告解析各自承担单一职责。

## 字段契约

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `summary` | `String` | 必填，不能为空 |
| `evidence` | `List<String>` | 必填，至少 1 条，每条不能为空 |
| `nextActions` | `List<String>` | 必填，至少 1 条，每条不能为空 |
| `riskLevel` | `RiskLevel` | 必填，只允许 `LOW`、`MEDIUM`、`HIGH` |

## 失败处理

| 场景 | 当前处理 |
| --- | --- |
| 模型输出不是 JSON | `DiagnosticReportParseException` |
| 缺少必填字段 | `DiagnosticReportParseException` |
| 数组为空 | `DiagnosticReportParseException` |
| 风险等级非法 | `DiagnosticReportParseException` |
| 第一次解析失败 | `DiagnosticReportGenerator` 带错误原因重试 |
| 超过最大次数仍失败 | 抛出 `DiagnosticReportParseException` |

## TDD 过程证据

第一轮红灯：先写 `DiagnosticReportParserTest`。

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=DiagnosticReportParserTest

失败原因：
找不到符号 DiagnosticReportParser
```

第一轮绿灯：

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=DiagnosticReportParserTest

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

第二轮红灯：先写 `DiagnosticReportGeneratorTest`。

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=DiagnosticReportGeneratorTest

失败原因：
找不到符号 DiagnosticReportGenerator
```

第二轮绿灯：

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=DiagnosticReportGeneratorTest

Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

完整验证：

```text
mvn -f ".../agent-app/pom.xml" test

Tests run: 9, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

被跳过的 1 个测试是 Day 06 的真实 DeepSeek smoke test，默认不会调用真实 API。

## 和后续 Day 的关系

```text
Day 06：模型返回普通文本
Day 07：普通文本必须能解析成诊断报告对象
Day 08：理解模型如何选择工具和生成工具参数
Day 09-10：实现本地只读工具
Day 11-15：把本地只读工具迁移到 MCP
```

## 常见误区

- 误区一：模型输出看起来像 JSON，就直接信任。
- 误区二：只校验 JSON 语法，不校验必填字段和枚举值。
- 误区三：把结构化输出误认为 Tool Calling。
- 误区四：结构化报告里混入真实工具执行逻辑。
- 误区五：解析失败后静默返回空报告，导致后续 Agent 基于错误状态继续运行。

## 验收标准

完成 Day 07 时，你应该能解释并验证：

- `DiagnosticReport` 为什么是排障 Agent 的输出契约。
- `summary / evidence / nextActions / riskLevel` 各自承担什么语义。
- 为什么 JSON 解析成功不代表诊断结论真实。
- 为什么解析失败要暴露为受控异常，而不是吞掉。
- 结构化输出、Tool Calling 和普通文本回答的区别。

## 复习问题

Day 07 直接关系到 Agent 输出边界，建议回答：

1. 为什么结构化输出只能保证“形状可解析”，不能保证“结论可信”？
2. 如果模型返回了合法 JSON，但 `evidence` 是它自己猜的，后续应该靠什么机制补上证据来源？
3. 为什么 `DiagnosticReportGenerator` 不应该直接实现 `search_code` 或 `git_history`？
