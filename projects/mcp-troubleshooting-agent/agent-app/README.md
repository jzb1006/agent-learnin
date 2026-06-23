# agent-app

`agent-app` 负责排障 Agent 的应用层编排。

## 负责

- 加载 system instruction 和 developer instruction。
- 接收用户排障问题。
- 调用 LLM API。
- 决定需要哪些只读证据。
- 调用模型、MCP Client 或本地适配层。
- 汇总证据并生成诊断报告。
- 写入 trace 和评测所需的结构化观察结果。

## 不负责

- 不直接实现代码搜索、Git 查询或配置读取细节。
- 不绕过 MCP Server 访问目标项目。
- 不保存具体业务事实和历史故障知识。
- 不执行重启、部署、改配置、写数据库或生产 API 调用。

## 设计原则

- KISS：先保留一个诊断入口，不提前做多 Agent 编排。
- YAGNI：未接入真实 LLM 前，不创建复杂抽象层。
- DRY：报告结构、工具结果和 trace 字段后续统一定义。
- SOLID：Agent 编排依赖工具契约，不依赖具体工具实现。

## 当前代码

Day 06 已新增最小 LLM API 调用边界：

```text
src/main/java/io/github/jiangzhibin/agentlearning/llm/
  ChatModelClient.java
  ChatModelProperties.java
  ChatModelResponse.java
  ChatModelException.java
  OpenAiCompatibleChatModelClient.java
```

Day 07 已新增结构化诊断报告边界：

```text
src/main/java/io/github/jiangzhibin/agentlearning/report/
  DiagnosticReport.java
  DiagnosticReportGenerator.java
  DiagnosticReportParser.java
  DiagnosticReportParseException.java
  RiskLevel.java
```

当前支持 OpenAI-compatible 非流式文本调用，以及把模型输出解析为 `summary / evidence / nextActions / riskLevel` 结构化报告。

仍不包含 Tool Calling、MCP Client、RAG、Memory、Agent Loop 或 CLI / REST 入口。

## 本地验证

默认测试只使用本地 fake API，不会调用真实模型：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test
```

真实 DeepSeek smoke test 需要显式启用：

```bash
export DEEPSEEK_API_KEY="你的真实 key"
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" \
  test -Dtest=DeepSeekChatModelClientSmokeTest -Ddeepseek.smoke=true
```
