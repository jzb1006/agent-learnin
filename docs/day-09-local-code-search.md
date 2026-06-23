# Day 09：实现本地只读工具 search_code

## 今日节点

Day 09：实现本地只读工具 `search_code`。

## 今日主题

把 Day 08 的工具调用契约落到一个真实只读工具上：按关键词搜索允许根目录内的 Java 源码，并返回可追溯的文件路径、行号和代码片段。

## 今天解决的问题

- `search_code` 应该放在哪一层。
- 工具如何限制输入参数。
- 工具如何保证不能越过允许根目录。
- 搜索结果如何裁剪，避免把大量代码塞进模型上下文。
- 没有匹配、参数错误、执行失败应该如何表达。
- 如何用 TDD 验证工具边界。

## 今天不解决的问题

- 不做语义检索、Embedding、RAG 或重排。
- 不搜索非 Java 文件。
- 不支持正则表达式、复杂查询语法或多关键词组合。
- 不读取配置、Git 历史或日志。
- 不接入 MCP Server，迁移到 MCP 放到 Day 13。
- 不做任何写操作、重启、部署、配置修改或生产 API 调用。

## 一句话定义

`search_code` 是一个本地只读代码搜索工具：它接收一个关键词，只在允许根目录内扫描 `.java` 文件，并把匹配行作为 `ToolEvidence` 返回。

## 概念讲解

### search_code 解决什么

排障 Agent 不能只靠模型猜测“代码里大概有某个类或配置”。它需要先拿到证据：

```json
{
  "toolName": "search_code",
  "arguments": {
    "keyword": "HikariPool"
  }
}
```

工具执行后返回：

```json
{
  "status": "SUCCESS",
  "summary": "找到 1 个匹配片段",
  "evidence": [
    {
      "source": "src/main/java/com/example/App.java:2",
      "content": "void connect() { System.out.println(\"HikariPool timeout\"); }"
    }
  ]
}
```

这样后续诊断报告可以引用“哪个文件第几行出现了什么”，而不是生成没有来源的结论。

### search_code 不解决什么

`search_code` 只是关键词搜索，不理解业务语义。

它不能证明：

- 某段代码一定会在运行时执行。
- 某个问题的根因一定在匹配文件里。
- 没搜到就代表代码不存在相关逻辑。
- 匹配越多就代表优先级越高。

所以它只提供证据片段，不负责最终推理。推理仍属于 Agent 编排和诊断报告层。

### 和 Java 后端开发的类比

`search_code` 类似一个只读 Repository 查询方法：

```text
CodeSearchTool.search(keyword)
  -> 只查允许范围
  -> 返回有限结果
  -> 不修改任何状态
  -> 查询失败时返回受控错误
```

区别是，参数通常来自模型生成，所以服务端必须重新校验参数、路径和结果大小。

## 项目映射

代码落点：

```text
projects/mcp-troubleshooting-agent/agent-app/
  src/main/java/io/github/jiangzhibin/agentlearning/tool/
    LocalCodeSearchTool.java
    TroubleshootingTool.java
    ToolCall.java
    ToolResult.java
    ToolEvidence.java
  src/test/java/io/github/jiangzhibin/agentlearning/tool/
    LocalCodeSearchToolTest.java
```

职责划分：

```text
TroubleshootingTool
  定义工具接口。

LocalCodeSearchTool
  实现本地 Java 源码搜索。

ToolCall
  承载模型生成的工具名和 keyword 参数。

ToolResult
  承载成功证据或失败语义。

ToolEvidence
  承载 source 和 content，保证证据可追溯。
```

## 实现边界

`LocalCodeSearchTool` 当前实现遵守以下规则：

| 规则 | 当前实现 |
| --- | --- |
| 工具名 | 只接受 `search_code` |
| 参数 | 只接受 `keyword` |
| 关键词空值 | 返回 `INVALID_ARGUMENTS` |
| 关键词长度 | 超过 128 字符返回 `INVALID_ARGUMENTS` |
| 允许根目录 | 构造时传入 `allowedRoot`，并转成真实路径 |
| 文件范围 | 只扫描 `.java` 普通文件 |
| 符号链接 | 跳过符号链接，避免借链接越界读取 |
| 结果上限 | 最多返回 5 个匹配片段 |
| 空结果 | 返回成功结果，证据内容说明“未找到匹配片段” |
| 执行异常 | 返回 `EXECUTION_FAILED` |

## TDD 过程证据

### 红灯 1：生产类不存在

先写 `LocalCodeSearchToolTest`，覆盖正常搜索、空关键词、错误工具名、结果裁剪和符号链接越界。

```text
mvn -f ".../agent-app/pom.xml" -Dtest=LocalCodeSearchToolTest test

失败原因：
找不到符号 LocalCodeSearchTool
```

### 绿灯 1：补最小搜索实现

新增 `LocalCodeSearchTool`，实现 `TroubleshootingTool`。

```text
mvn -f ".../agent-app/pom.xml" -Dtest=LocalCodeSearchToolTest test

Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 红灯 2：补输入长度限制

继续补一个 129 字符关键词测试。

```text
mvn -f ".../agent-app/pom.xml" -Dtest=LocalCodeSearchToolTest#shouldRejectOverlongKeyword test

失败原因：
expected: <INVALID_ARGUMENTS> but was: <SUCCESS>
```

### 绿灯 2：限制 keyword 最大长度

实现 128 字符上限。

```text
mvn -f ".../agent-app/pom.xml" -Dtest=LocalCodeSearchToolTest test

Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 全量回归

```text
mvn -f ".../agent-app/pom.xml" test

Tests run: 21, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

跳过的 1 个测试是需要真实模型环境变量的 `DeepSeekChatModelClientSmokeTest`。

## 今日决策

采用“本地 Java 工具实现 + 严格边界测试”的方案。

原因：

- KISS：先做关键词搜索，不提前引入全文索引、Embedding 或 MCP transport。
- YAGNI：当前只需要验证本地只读工具闭环，不实现复杂查询语言。
- DRY：继续复用 Day 08 的 `ToolCall / ToolResult / ToolEvidence`。
- SOLID：Agent 编排层依赖 `TroubleshootingTool`，后续可以把本地实现替换为 MCP Tool 适配。

## 和后续 Day 的关系

```text
Day 08：定义工具调用契约
Day 09：实现 search_code 本地只读工具
Day 10：实现 git_history 和 read_config
Day 13：把 search_code 接入 MCP
Day 29：压缩 search_code 的长结果
```

## 常见误区

- 误区一：只读工具没有安全风险，所以不用限制路径。
- 误区二：把用户或模型传来的路径直接拼到文件系统里。
- 误区三：搜索结果越多越好，忽略上下文窗口成本。
- 误区四：没搜到就当成“代码中不存在”。
- 误区五：把搜索工具做成业务推理工具，让它直接判断根因。

## 复盘提问

1. 为什么 `search_code` 要跳过符号链接？
2. 为什么搜索结果需要裁剪到固定数量，而不是全部返回给模型？
3. 为什么“未找到匹配片段”可以作为 observation，但不能被当成绝对结论？

## 验收标准

完成 Day 09 时，你应该能解释并验证：

- 输入关键词能返回相关 Java 文件路径、行号和片段。
- 空关键词、超长关键词和错误工具名会返回 `INVALID_ARGUMENTS`。
- 搜索不会读取允许根目录外的符号链接目标。
- 结果数量会裁剪，避免污染模型上下文。
- 工具仍然只返回事实证据，不负责最终根因判断。
