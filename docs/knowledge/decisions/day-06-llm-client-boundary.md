# Day 06 决策记录：先用 JDK HttpClient 实现 OpenAI-compatible 模型客户端

## 背景

Day 05 已经确定模型配置、环境变量和运行入口草案。Day 06 需要让 `agent-app` 能完成最小 LLM API 调用。

当前外部模型以 DeepSeek 为默认验证目标。DeepSeek 提供 OpenAI-compatible Chat API，因此代码层可以保留通用模型客户端边界，而不是把 Agent 编排绑定到 DeepSeek 专用实现。

## 决策

在 `agent-app` 中新增 `ChatModelClient` 接口，并用 JDK `HttpClient` 实现 `OpenAiCompatibleChatModelClient`。

当前只支持：

- 非流式 Chat Completion。
- 单个 user message。
- 返回 `choices[0].message.content`。
- HTTP 错误、超时和解析失败转成 `ChatModelException`。
- API Key 通过 `ChatModelProperties` 注入，不在客户端内部读取环境变量。

## 备选方案

### 方案 A：JDK HttpClient

优点：

- 依赖少。
- 能看清请求和响应结构。
- 与 Day 06 学习目标完全对齐。

缺点：

- 后续结构化输出、工具调用和观测要继续扩展。

### 方案 B：Spring AI ChatClient

优点：

- 和后续主线框架一致。
- 更容易接入 Spring Observability。

缺点：

- Day 06 会过早进入 Spring Boot 配置和 Bean 生命周期。
- 学习重点容易从 LLM API 偏移到框架使用。

### 方案 C：第三方 OpenAI Java SDK

优点：

- 少写 HTTP 和 JSON 解析代码。

缺点：

- 抽象遮住了 Chat API 的实际形状。
- 后续迁移 Spring AI 可能产生重复封装。

## 结论

采用方案 A。

Day 06 先用最小依赖打通模型调用边界，后续 Day 07 再扩展结构化输出，Day 08 以后再引入工具调用，Spring AI 集成在理解底层调用后再评估。

## 影响

- Agent 编排代码后续只依赖 `ChatModelClient`。
- 切换 OpenAI-compatible 供应商时优先改配置，不改 Agent 业务逻辑。
- 真实 API 调用通过显式 smoke test 验证，日常测试不依赖网络和 API Key。
- 后续接入 Spring AI 时，可以保留 `ChatModelClient` 作为应用层端口。

## 验证方式

- 本地 fake API 测试成功路径。
- 本地 fake API 测试 HTTP 错误。
- 本地 fake API 测试超时。
- `mvn test` 默认不调用真实 API。
- 真实 DeepSeek smoke test 必须通过 `-Ddeepseek.smoke=true` 显式启用。
