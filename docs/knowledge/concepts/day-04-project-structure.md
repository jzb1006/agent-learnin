# Day 04：项目结构与模块边界

## 一句话定义

项目结构是 Agent 工程的职责边界图，用目录和说明提前约束推理、工具、知识、评测和 trace 的归属。

## 解决的问题

它解决的是后续能力堆叠时的职责混乱问题。

如果没有清晰结构，排障 Agent 很容易变成：

- Agent 入口直接读文件、查 Git、拼报告。
- Prompt 里混入业务知识和工具规则。
- 工具返回、报告格式和 trace 字段各自为政。
- RAG、Memory、Evals 后续没有稳定落点。

## 不解决的问题

项目结构本身不解决：

- LLM API 调用。
- Tool Calling 执行。
- MCP 协议实现。
- RAG 检索质量。
- Memory 管理。
- 生产级观测和评测。

它只是为这些能力预留清晰位置。

## 在本项目中的位置

Day 04 创建了：

```text
projects/mcp-troubleshooting-agent/
  agent-app/
  mcp-server/
  knowledge-base/
  evals/
  traces/
```

模块职责：

- `agent-app`：Agent 编排、模型调用和报告生成。
- `mcp-server`：MCP Tools、Resources、Prompts、Roots 和访问边界。
- `knowledge-base`：本地文档、手册、历史案例。
- `evals`：格式、证据、安全和工具失败评测。
- `traces`：排障过程和工具调用链路记录。

## 最小代码证据

Day 04 不写 Java 代码。

今天的最小证据是：

- `projects/mcp-troubleshooting-agent/README.md`
- 各子目录 `README.md`
- Day 04 学习文档和知识沉淀文件

## 常见误区

- 误区一：目录越多越工程化。
- 误区二：先把 Spring Boot 多模块工程建好再说。
- 误区三：Agent 应用只有一个聊天入口，不需要 evals 和 traces。
- 误区四：所有读取能力都应该做成 Tool。
- 误区五：知识库可以保存未经验证的推断和敏感信息。

## 自测问题

1. `agent-app` 和 `mcp-server` 为什么要分开？
2. MCP Tool、Resource、Prompt 的边界分别是什么？
3. `knowledge-base` 为什么不能替代当前代码和配置读取工具？
4. `evals` 在没有真实模型前能评测什么？
5. `traces` 和长期 Memory 有什么区别？

## 今日结论

Day 04 的核心是先把工程边界立住。目录不是形式主义，而是为后续 LLM、Tool Calling、MCP、RAG、Memory、Evals 保留可演进的位置。
