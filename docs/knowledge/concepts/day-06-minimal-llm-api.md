# Day 06：最小 LLM API 调用

## 一句话定义

最小 LLM API 调用是把用户问题通过模型客户端发送给 Chat API，并把模型返回的助手文本转换成应用可使用的结果对象。

## 解决的问题

它解决的是 Agent 工程的第一层能力问题：应用必须先能稳定调用模型，后续才谈得上结构化输出、Tool Calling、MCP、RAG 和 Agent 编排。

在本项目中，Day 06 重点解决：

- 模型配置隔离。
- 非流式文本调用。
- 超时控制。
- HTTP 错误处理。
- 响应解析。
- API Key 不进入代码和测试输出。

## 不解决的问题

Day 06 不解决：

- 结构化诊断报告。
- JSON schema 校验。
- Tool Calling。
- MCP 工具调用。
- RAG 检索。
- Agent 多轮编排。
- CLI / REST 运行入口。

## 在本项目中的位置

```text
agent-app
  llm
    ChatModelClient
    OpenAiCompatibleChatModelClient
    ChatModelProperties
    ChatModelResponse
    ChatModelException
```

`agent-app` 负责模型调用和 Agent 编排，因此 `ChatModelClient` 放在这里。

`mcp-server` 不负责模型推理，它只负责后续暴露只读工具、资源和访问边界。

## 最小代码证据

生产代码：

- `projects/mcp-troubleshooting-agent/agent-app/src/main/java/io/github/jiangzhibin/agentlearning/llm/ChatModelClient.java`
- `projects/mcp-troubleshooting-agent/agent-app/src/main/java/io/github/jiangzhibin/agentlearning/llm/OpenAiCompatibleChatModelClient.java`
- `projects/mcp-troubleshooting-agent/agent-app/src/main/java/io/github/jiangzhibin/agentlearning/llm/ChatModelProperties.java`
- `projects/mcp-troubleshooting-agent/agent-app/src/main/java/io/github/jiangzhibin/agentlearning/llm/ChatModelResponse.java`
- `projects/mcp-troubleshooting-agent/agent-app/src/main/java/io/github/jiangzhibin/agentlearning/llm/ChatModelException.java`

测试代码：

- `projects/mcp-troubleshooting-agent/agent-app/src/test/java/io/github/jiangzhibin/agentlearning/llm/OpenAiCompatibleChatModelClientTest.java`
- `projects/mcp-troubleshooting-agent/agent-app/src/test/java/io/github/jiangzhibin/agentlearning/llm/DeepSeekChatModelClientSmokeTest.java`

验证命令：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test
```

验证结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 常见误区

- 把 API Key 写进源码、测试或文档示例的真实值里。
- 模型调用代码直接散落在 Controller、Service 或 Agent Loop 中。
- 成功路径能跑就算完成，不处理 HTTP 错误和超时。
- 单元测试依赖真实模型，导致测试慢、不稳定、不可离线运行。
- 在 Day 06 提前实现结构化输出和工具调用。

## 自测问题

Day 06 的自测只保留 Agent 相关问题。

1. `ChatModelClient` 这个边界对后续 Agent 编排有什么价值？
2. 为什么模型调用失败必须变成 Agent 能识别的受控失败？
3. Day 07 会在 Day 06 的普通文本回复基础上增加什么 Agent 能力？

## 今日结论

Day 06 的核心不是“让模型随便回一句话”，而是建立一个可测试、可替换、可失败定位的模型调用边界。
