# AI Agent 每日学习路径

## 学习目标

通过开发一个 MCP 优先的 Java 后端排障 Agent，系统串联 AI Agent 应用工程的完整脉络：

- LLM API：Agent 的基础推理与结构化输出
- Tool Calling：模型选择工具、传参、解析结果
- MCP：把工具能力标准化，而不是写死在 Prompt 或 Java 方法里
- RAG：检索接口文档、排障手册、历史案例
- Agent 编排：计划、执行、观察、迭代、总结
- 权限边界：只读工具默认放行，写操作后续加审批
- Observability / Evals：记录 trace、工具调用链、失败样本、评测集

## 项目主线

目标项目：

- `projects/mcp-troubleshooting-agent`

学习产物：

- 一个可运行的 MCP 优先排障 Agent
- 一组 MCP 只读工具
- 一套本地知识库和 RAG 检索流程
- 一条 Agent plan-act-observe-reflect-answer 编排链路
- 一套 trace、eval case 和学习复盘记录

第一阶段边界：

- 只实现只读能力
- 不执行重启、部署、改配置、写数据库等高风险操作
- 写操作只保留接口设计和审批流程说明

## Java 框架选型

主线推荐：

- Spring Boot + Spring AI
- Spring AI MCP / MCP Java SDK

选择理由：

- 与 Java 后端开发习惯一致，适合从 Spring Boot 工程自然进入 Agent 应用工程
- Spring AI 覆盖 LLM API、结构化输出、Tool Calling、RAG 和 Observability
- Spring AI MCP / MCP Java SDK 适合把排障工具标准化为 MCP 工具，而不是写死在 Agent 代码里
- 后续可以平滑接入真实 Java 项目的日志、配置、Git、接口文档和历史故障案例

本项目默认技术路线：

```text
主框架：Spring Boot + Spring AI
MCP：Spring AI MCP / MCP Java SDK
RAG：先用 Spring AI 简单检索，后续再评估 embedding / vector store
观测：Spring AI Observability + Micrometer / OpenTelemetry
评测：先自建 eval case，后续再对照 Google ADK 的评测思路
```

对照学习框架：

| 框架 | 学习定位 | 何时学习 |
| --- | --- | --- |
| Spring AI | 主线框架，负责 LLM API、Tool Calling、RAG、Observability | 从 Day 04 开始贯穿全程 |
| Spring AI MCP / MCP Java SDK | 主线 MCP 实现，负责 MCP Server 和 MCP Client | 第 3 周重点学习 |
| LangChain4j | 对照框架，重点比较 RAG、Tools、Memory、Agent 抽象 | 主线跑通后再学习 |
| Google ADK Java | Agent-first 对照框架，重点学习 Agent 编排、多 Agent 和评测思路 | 第 6 周后扩展学习 |
| Semantic Kernel Java | 企业级 Agent / Azure 生态对照框架 | 有 Azure 或企业集成需求时学习 |
| Quarkus LangChain4j | Quarkus / 云原生 / GraalVM 场景对照框架 | 非当前主线，后续按需学习 |

学习原则：

- 第一阶段只使用 Spring Boot + Spring AI + MCP，避免同时引入多个 Agent 框架
- LangChain4j、Google ADK Java 和 Semantic Kernel Java 只作为对照，不参与 MVP 实现
- 框架学习服务于排障 Agent 主线，不为了覆盖技术清单而引入复杂依赖

## 学习状态约定

- `[ ]` 未学习
- `[x]` 已学习

每天完成一个节点后，把对应条目标记为 `[x]`，并在「学习记录」里追加日期、节点和总结。

## 每日学习路径

### 第 1 周：Agent 基础与工程骨架

- [ ] Day 01：理解 AI Agent 与普通 LLM 应用的区别
  - 学习重点：LLM 应用、Tool Calling 应用、Agent 应用的边界
  - 实践产出：写出本项目的 Agent 工作流图
  - 验收标准：能解释为什么排障场景适合 Agent

- [ ] Day 02：明确排障 Agent 的 MVP 范围
  - 学习重点：KISS、YAGNI、只读边界、真实项目接入范围
  - 实践产出：定义首个用户问题和预期诊断报告格式
  - 验收标准：MVP 不包含高风险写操作

- [ ] Day 03：初始化项目结构
  - 学习重点：模块边界、职责划分、Agent 与工具解耦
  - 实践产出：创建 `agent-app`、`mcp-server`、`knowledge-base`、`evals`、`traces`
  - 验收标准：目录结构能支撑后续七层能力扩展

- [ ] Day 04：实现最小 LLM API 调用
  - 学习重点：模型客户端、超时、错误处理、配置隔离
  - 实践产出：`ChatModelClient`
  - 验收标准：能输入问题并得到模型回复

- [ ] Day 05：实现结构化输出
  - 学习重点：JSON schema、响应解析、字段校验
  - 实践产出：诊断报告结构 `summary / evidence / nextActions / riskLevel`
  - 验收标准：模型输出可以稳定解析为 Java 对象

### 第 2 周：Tool Calling

- [ ] Day 06：理解 Tool Calling 的执行模型
  - 学习重点：工具 schema、参数、返回值、失败语义
  - 实践产出：设计工具接口规范
  - 验收标准：能说明工具调用和普通文本回答的区别

- [ ] Day 07：实现第一个只读工具 `search_code`
  - 学习重点：代码搜索、输入限制、结果裁剪
  - 实践产出：按关键词搜索目标 Java 项目源码
  - 验收标准：输入关键词能返回相关文件路径和片段

- [ ] Day 08：实现 `git_history`
  - 学习重点：Git 查询、时间范围、提交摘要
  - 实践产出：查询最近提交、按关键词过滤提交
  - 验收标准：能定位与故障关键词相关的提交

- [ ] Day 09：实现 `read_config`
  - 学习重点：配置读取、敏感值脱敏、只读边界
  - 实践产出：读取目标项目配置文件并脱敏输出
  - 验收标准：不会泄露 token、secret、password

- [ ] Day 10：实现工具调用结果标准化
  - 学习重点：Observation、错误可恢复、工具结果摘要
  - 实践产出：统一 `ToolResult`
  - 验收标准：所有工具成功和失败都返回一致结构

### 第 3 周：MCP

- [ ] Day 11：理解 MCP 的角色
  - 学习重点：MCP Server、MCP Client、tool list、tool call
  - 实践产出：画出 Agent 到 MCP Server 的调用链
  - 验收标准：能解释为什么工具不直接写进 Agent

- [ ] Day 12：搭建最小 MCP Server
  - 学习重点：工具注册、协议边界、启动方式
  - 实践产出：MCP Server 暴露一个 `ping` 工具
  - 验收标准：Agent 侧能发现并调用 `ping`

- [ ] Day 13：把 `search_code` 接入 MCP
  - 学习重点：工具迁移、schema 对齐、结果序列化
  - 实践产出：通过 MCP 调用代码搜索
  - 验收标准：Agent 不再直接依赖本地代码搜索实现

- [ ] Day 14：把 `git_history` 和 `read_config` 接入 MCP
  - 学习重点：多工具注册、工具命名、错误隔离
  - 实践产出：MCP Server 暴露三个只读工具
  - 验收标准：工具列表、参数 schema、返回值均可验证

- [ ] Day 15：实现 MCP 工具权限分类
  - 学习重点：只读工具、写操作工具、高风险工具
  - 实践产出：工具元数据包含 `riskLevel`
  - 验收标准：只读工具默认允许，高风险工具不会执行

### 第 4 周：RAG

- [ ] Day 16：整理知识库结构
  - 学习重点：文档来源、知识切分、引用路径
  - 实践产出：`knowledge-base` 下放入接口文档、排障手册、历史案例
  - 验收标准：每份知识都有来源路径

- [ ] Day 17：实现最小全文检索
  - 学习重点：先用简单方案打通闭环
  - 实践产出：`retrieve_docs` 支持关键词检索
  - 验收标准：能返回相关文档片段和来源

- [ ] Day 18：把 `retrieve_docs` 接入 MCP
  - 学习重点：RAG 工具化、检索结果压缩
  - 实践产出：Agent 通过 MCP 检索知识库
  - 验收标准：诊断报告能引用知识库来源

- [ ] Day 19：引入 embedding 或向量库方案评估
  - 学习重点：全文检索、embedding、向量检索、重排的取舍
  - 实践产出：写出是否引入向量库的决策记录
  - 验收标准：不为了技术完整性过早引入复杂依赖

- [ ] Day 20：实现 RAG 回答引用约束
  - 学习重点：证据优先、引用来源、防止幻觉
  - 实践产出：回答中包含 `source`
  - 验收标准：没有来源的结论必须标记为推断

### 第 5 周：Agent 编排

- [ ] Day 21：设计 Agent 状态模型
  - 学习重点：Plan、Act、Observe、Reflect、Answer
  - 实践产出：定义 Agent 状态对象
  - 验收标准：每一轮执行都能被序列化记录

- [ ] Day 22：实现 Plan 阶段
  - 学习重点：结构化计划、步骤拆分、工具候选
  - 实践产出：用户输入故障现象后生成排障计划
  - 验收标准：计划包含要查什么和为什么查

- [ ] Day 23：实现 Act 和 Observe 阶段
  - 学习重点：工具选择、参数生成、结果归档
  - 实践产出：Agent 能按计划调用 MCP 工具
  - 验收标准：每次工具调用都有输入、输出、状态

- [ ] Day 24：实现 Reflect 阶段
  - 学习重点：是否继续、是否缺证据、是否收敛
  - 实践产出：Agent 根据观察结果决定下一步
  - 验收标准：有最大轮数限制，避免无限循环

- [ ] Day 25：实现最终诊断报告
  - 学习重点：结论、证据、风险、建议分离
  - 实践产出：输出完整排障报告
  - 验收标准：报告能区分事实、推断和建议

### 第 6 周：权限边界、观测与评测

- [ ] Day 26：设计高风险操作审批机制
  - 学习重点：危险操作识别、人工确认、审计记录
  - 实践产出：写操作工具接口设计，但不执行真实操作
  - 验收标准：重启、部署、改配置必须进入审批流程

- [ ] Day 27：实现 trace 记录
  - 学习重点：可观测性、调用链、latency、错误记录
  - 实践产出：每次 Agent 运行写入 `traces`
  - 验收标准：能还原一次完整排障过程

- [ ] Day 28：实现成本和失败样本记录
  - 学习重点：token、耗时、失败原因、样本沉淀
  - 实践产出：记录模型调用和工具失败
  - 验收标准：失败不是丢日志，而是进入复盘材料

- [ ] Day 29：建立 eval case
  - 学习重点：输入、期望工具、期望证据、期望结论
  - 实践产出：至少 5 个排障评测用例
  - 验收标准：能重复运行并判断 Agent 是否退化

- [ ] Day 30：完整端到端验收
  - 学习重点：从用户问题到诊断报告的完整闭环
  - 实践产出：运行真实故障问题排障流程
  - 验收标准：LLM API、Tool Calling、MCP、RAG、编排、权限、trace、eval 全部串联

## 推荐每日节奏

每次学习控制在 60 到 90 分钟：

1. 10 分钟：复盘上一个节点
2. 20 分钟：学习当天概念
3. 40 分钟：实现或整理当天产出
4. 10 分钟：记录问题和下一步

每天只追求完成一个清晰节点，不跨太多主题。

## 学习记录

| 日期 | 节点 | 状态 | 总结 |
| --- | --- | --- | --- |
| 2026-06-22 | 初始化学习路径 | 已完成 | 确定以 MCP 优先排障 Agent 串联完整 AI Agent 学习主线。 |

## 当前进度

- 总节点数：30
- 已学习：0
- 当前建议节点：Day 01

## 后续标记方式

你可以直接说：

- `Day 01 学完了`
- `把 Day 03 标记为已学习`
- `今天学到 Tool Calling 了，帮我更新进度`

我会更新本文档中的 checkbox、学习记录和当前进度。
