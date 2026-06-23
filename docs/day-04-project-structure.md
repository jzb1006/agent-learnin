# Day 04：初始化项目结构

## 今日节点

Day 04：初始化项目结构。

## 今日主题

为 MCP 优先的 Java 后端排障 Agent 建立第一版目录骨架，并明确 `agent-app`、`mcp-server`、`knowledge-base`、`evals`、`traces` 的职责边界。

## 今天解决的问题

- 为什么初始化结构比直接写代码更重要。
- Agent、MCP Server、知识库、评测和 trace 分别放在哪一层。
- 如何避免把工具实现、Prompt、业务知识和报告逻辑写成一团。
- 第一阶段目录如何支撑后续 LLM、MCP、RAG、Memory、Evals 扩展。

## 今天不解决的问题

- 不创建 Spring Boot 工程。
- 不引入 Spring AI 依赖。
- 不实现真实 LLM API 调用。
- 不实现 MCP 协议代码。
- 不实现 RAG、Memory、多 Agent 编排。
- 不执行重启、部署、改配置、写数据库或生产 API 调用。

## 一句话定义

项目结构是 Agent 工程的职责边界图：它提前规定“谁负责推理、谁负责工具、谁负责知识、谁负责评测、谁负责记录过程”，避免后续把所有能力堆进一个入口类或一个 Prompt。

## 当前项目结构

```text
projects/mcp-troubleshooting-agent/
  README.md
  agent-app/
    README.md
  mcp-server/
    README.md
  knowledge-base/
    README.md
  evals/
    README.md
  traces/
    README.md
```

## 模块职责

| 模块 | 负责 | 不负责 |
| --- | --- | --- |
| `agent-app` | Agent 编排、模型调用、报告生成、trace 写入 | 不直接实现底层代码搜索、配置读取、Git 查询 |
| `mcp-server` | MCP Tools、Resources、Prompts、Roots、输入校验、脱敏 | 不决定最终诊断结论，不做自然语言推理 |
| `knowledge-base` | 接口文档、排障手册、历史案例、模块说明 | 不保存敏感数据，不替代工具读取当前代码 |
| `evals` | 报告格式、证据引用、安全边界和工具失败评测 | 不追求单个样例看起来聪明 |
| `traces` | 工具调用链路、观察结果、最终报告摘要 | 不保存敏感配置明文和完整长日志 |

## 为什么要这样拆

### 1. Agent 与工具解耦

`agent-app` 负责“应该查什么、如何组织证据、如何输出报告”。

`mcp-server` 负责“能查什么、参数怎么校验、结果怎么返回、访问边界在哪里”。

这样做的好处是后续把本地 Java 方法迁移到 MCP Tool 时，Agent 的编排思路不用重写。

### 2. Prompt 不承载业务事实

Day 03 已确定 Prompt 只定义行为契约，不保存业务事实。

因此业务事实要进入：

- `knowledge-base`：稳定文档、手册、历史案例。
- `mcp-server` Resources：可作为上下文读取的项目元信息或文档摘要。
- `mcp-server` Tools：需要主动查询的当前代码、配置、Git 历史。

### 3. 评测和 trace 从第一天保留位置

Agent 应用的问题不是“能不能回答一次”，而是“能不能在变化后持续稳定”。

所以 `evals` 和 `traces` 不是后期补丁，而是从骨架阶段就存在：

- `evals` 用来验证格式、证据和安全边界。
- `traces` 用来复盘每次工具调用和观察链路。

### 4. MCP 原语边界提前固定

Day 04 不实现 MCP，但先固定建模规则：

- 主动查询或计算：MCP Tool。
- 稳定只读上下文：MCP Resource。
- 可复用排障流程：MCP Prompt。
- 访问目录边界：MCP Roots 和服务端路径校验。

这能避免后续把所有读取都做成 Tool，或者把所有流程都塞进 Prompt。

## 项目映射

后续能力落点如下：

```text
LLM API
  -> agent-app

Structured Output
  -> agent-app
  -> evals

Tool Calling
  -> agent-app 调用工具契约
  -> mcp-server 提供工具实现

MCP
  -> mcp-server

RAG
  -> knowledge-base
  -> agent-app 检索编排
  -> evals 验证引用质量

Memory / Context Compression
  -> agent-app 管理短期记忆
  -> traces 提供压缩输入

Observability / Evals
  -> traces
  -> evals
```

## KISS / YAGNI / DRY / SOLID 落地

KISS：今天只建立五个核心模块，不创建复杂子目录和抽象接口。

YAGNI：暂不创建 Spring Boot 工程、MCP Server 代码、向量库或多 Agent 目录。

DRY：每个模块只写一份职责说明，后续代码按这些边界扩展。

SOLID：按职责拆分模块，Agent 编排依赖工具契约，不依赖具体工具实现。

## 验收标准

完成 Day 04 时，你应该能解释：

- 为什么 `agent-app` 不应该直接实现所有工具逻辑。
- 为什么 `knowledge-base` 和 `mcp-server` Resources 不是同一件事。
- 为什么 `evals` 和 `traces` 要在项目早期就创建。
- 为什么 Day 04 不应该顺手创建 Spring Boot 工程。
- 当前目录结构如何支撑 LLM、MCP、RAG、Memory、Evals 后续扩展。

## 复习问题

1. `agent-app` 和 `mcp-server` 的核心边界是什么？
2. 一个“读取接口文档摘要”的能力更适合建模为 MCP Tool 还是 MCP Resource？为什么？
3. 为什么 trace 不能保存敏感配置明文和完整长日志？
4. 如果今天直接创建完整 Spring Boot 多模块工程，会违反哪个原则？风险是什么？
5. `evals` 在还没有模型调用时有什么价值？
