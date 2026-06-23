# Day 06：实现最小 LLM API 调用

## 今日节点

Day 06：实现最小 LLM API 调用。

## 今日主题

在 `agent-app` 中实现一个最小的 OpenAI-compatible `ChatModelClient`，让排障 Agent 能把用户问题发送给模型，并拿到非流式文本回复。

## 今天解决的问题

- 模型客户端应该放在哪一层。
- 最小 Chat API 请求和响应长什么样。
- 为什么 API Key、base url、model name 和 timeout 必须通过配置隔离。
- 模型调用失败时如何转成上层可识别的受控异常。
- 如何用本地 fake API 验证模型客户端，不依赖真实 API Key。

## 今天不解决的问题

- 不实现结构化诊断报告，放到 Day 07。
- 不实现 Tool Calling，放到 Day 08 以后。
- 不接入 MCP Client / Server。
- 不实现 CLI 或 REST 入口。
- 不引入多 Agent 编排、RAG、Memory 或 trace 持久化。
- 不默认调用真实 DeepSeek API，真实 smoke test 必须显式启用。

## 一句话定义

最小 LLM API 调用就是把“用户问题 -> HTTP Chat 请求 -> 模型文本回复”打通，同时保证模型配置不写死、超时可控、错误可解释、敏感值不泄露。

## 官方接口依据

DeepSeek 当前提供 OpenAI-compatible API：

- OpenAI-compatible base url：`https://api.deepseek.com`
- Chat endpoint：`POST /chat/completions`
- 当前模型：`deepseek-v4-flash`、`deepseek-v4-pro`
- 旧模型名 `deepseek-chat`、`deepseek-reasoner` 将在 `2026-07-24 15:59 UTC` 停用

参考：

- https://api-docs.deepseek.com/
- https://api-docs.deepseek.com/api/create-chat-completion

## 设计方案

### 方案 A：直接使用 JDK `HttpClient`

优点：

- 依赖少，能看清 Chat API 的真实请求结构。
- 适合 Day 06 学习模型调用、超时和错误处理。
- 不提前引入 Spring Boot / Spring AI 样板。

缺点：

- 后续结构化输出、观测和框架集成需要自己扩展。

### 方案 B：直接使用 Spring AI `ChatClient`

优点：

- 更贴近后续主线框架。
- 后续接入结构化输出、观测和工具调用更顺。

缺点：

- Day 06 会被 Spring Boot 配置、Bean 生命周期和 starter 依赖稀释。
- 学习者不容易看清底层 Chat API 形状。

### 方案 C：引入第三方 OpenAI Java SDK

优点：

- 少写 HTTP 和 JSON 解析代码。

缺点：

- 依赖外部 SDK 抽象，学习价值偏低。
- 后续迁移 Spring AI 时会多一层过渡成本。

## 今日决策

采用方案 A：先用 JDK `HttpClient` 实现最小 OpenAI-compatible 客户端。

原因：

- KISS：只实现当前需要的非流式文本调用。
- YAGNI：不提前做结构化输出、Tool Calling 或 Spring AI 集成。
- DRY：所有模型调用先统一经过 `ChatModelClient`。
- SOLID：Agent 应用层依赖 `ChatModelClient` 接口，不依赖具体 DeepSeek HTTP 实现。

## 代码落点

```text
projects/mcp-troubleshooting-agent/agent-app/
  pom.xml
  src/main/java/io/github/jiangzhibin/agentlearning/llm/
    ChatModelClient.java
    ChatModelProperties.java
    ChatModelResponse.java
    ChatModelException.java
    OpenAiCompatibleChatModelClient.java
  src/test/java/io/github/jiangzhibin/agentlearning/llm/
    OpenAiCompatibleChatModelClientTest.java
    DeepSeekChatModelClientSmokeTest.java
```

## 最小调用链

```text
用户问题
  -> ChatModelClient.complete(question)
  -> OpenAiCompatibleChatModelClient
  -> POST {baseUrl}/chat/completions
  -> Authorization: Bearer ${DEEPSEEK_API_KEY}
  -> model + messages + stream=false
  -> choices[0].message.content
  -> ChatModelResponse
```

## 错误处理边界

| 场景 | 当前处理 |
| --- | --- |
| 用户问题为空 | 抛出 `IllegalArgumentException` |
| 配置缺失 | `ChatModelProperties` 构造时失败 |
| HTTP 非 2xx | 抛出 `ChatModelException`，包含状态码和已脱敏错误 |
| 调用超时 | 抛出 `ChatModelException`，提示 timeout |
| 响应 JSON 无法解析 | 抛出 `ChatModelException` |
| 响应缺少文本 | 抛出 `ChatModelException` |

## TDD 过程证据

先写测试，再实现生产代码。

红灯证据：

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=OpenAiCompatibleChatModelClientTest

失败原因：
找不到符号 ChatModelProperties
```

绿灯证据：

```text
mvn -f ".../agent-app/pom.xml" test -Dtest=OpenAiCompatibleChatModelClientTest

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

完整验证：

```text
mvn -f ".../agent-app/pom.xml" test

Tests run: 4, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

被跳过的 1 个测试是 `DeepSeekChatModelClientSmokeTest`，默认不调用真实 API。

## 真实 API smoke test

真实 DeepSeek 调用需要你显式配置 API Key，并显式启用测试：

```bash
export DEEPSEEK_API_KEY="你的真实 key"
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" \
  test -Dtest=DeepSeekChatModelClientSmokeTest -Ddeepseek.smoke=true
```

注意：

- 这个命令会向 DeepSeek 发送网络请求。
- API Key 只从环境变量读取，不写入仓库。
- 当前自动化验证默认不会运行它。

## 和后续 Day 的关系

```text
Day 06：ChatModelClient 返回普通文本
Day 07：把模型输出稳定解析为 Java 诊断报告对象
Day 08：理解模型如何选择工具
Day 09-10：实现本地只读工具
Day 11-15：把只读工具迁移到 MCP
```

## 常见误区

- 误区一：直接在业务代码里拼 API Key。
- 误区二：把 `deepseek-v4-flash` 写散在多个类里。
- 误区三：只处理成功响应，不处理超时和 HTTP 错误。
- 误区四：单元测试直接打真实 API，导致测试不稳定且泄露风险更高。
- 误区五：Day 06 顺手做 JSON schema、Tool Calling 和 Agent Loop。

## 验收标准

完成 Day 06 时，你应该能解释并验证：

- `ChatModelClient` 为什么属于 `agent-app`。
- 为什么当前实现叫 OpenAI-compatible，而不是 DeepSeek 专用业务类。
- API Key 为什么只能来自环境变量或私有配置。
- 超时、HTTP 错误和响应解析失败如何被上层识别。
- 如何通过本地 fake API 测试模型客户端。
- 如何在显式确认后运行真实 DeepSeek smoke test。

## 复习问题

Day 06 的复习只聚焦 Agent 边界，不考普通 HTTP 客户端细节。

1. `ChatModelClient` 这个边界对后续 Agent 编排有什么价值？
2. 为什么模型调用失败必须变成 Agent 能识别的受控失败，而不是直接让异常散出去？
3. 为什么 Day 06 只返回普通文本，结构化诊断报告要留到 Day 07？
