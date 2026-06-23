# AI Agent 每日学习路径

## 学习目标

通过开发一个 MCP 优先的 Java 后端排障 Agent，系统串联 AI Agent 应用工程的完整脉络：

- LLM API：Agent 的基础推理与结构化输出
- Tool Calling：模型选择工具、传参、解析结果
- MCP：把工具能力标准化，而不是写死在 Prompt 或 Java 方法里
- RAG：检索接口文档、排障手册、历史案例
- Memory：管理短期会话记忆、长期项目记忆、历史故障案例
- Context Compression：压缩日志、工具结果和多轮上下文，控制上下文预算
- Agent 编排：计划、执行、观察、迭代、总结
- 权限与安全：只读工具默认放行，写操作后续加审批，防 Prompt Injection
- Observability / Evals / Testing：记录 trace、工具调用链、失败样本、评测集和回归测试

## 项目主线

目标项目：

- `projects/mcp-troubleshooting-agent`

学习产物：

- 一个可运行的 MCP 优先排障 Agent
- 一组 MCP 只读工具
- 一套本地知识库和 RAG 检索流程
- 一套短期记忆、长期记忆和上下文压缩策略
- 一条 Agent plan-act-observe-reflect-answer 编排链路
- 一套 trace、eval case、测试用例和学习复盘记录

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
记忆：先用本地文件或内存存储，后续再评估数据库 / 向量记忆
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

### 阶段总览

| 阶段 | 天数 | 主题 | 目标 |
| --- | --- | --- | --- |
| 第 1 周 | Day 01-05 | Agent 基础、Prompt、工程骨架 | 明确 Agent 边界、排障场景和项目结构 |
| 第 2 周 | Day 06-10 | LLM API 与 Tool Calling | 打通模型调用、结构化输出和本地只读工具 |
| 第 3 周 | Day 11-15 | MCP 基础与工具化 | 用 MCP 标准化暴露排障工具 |
| 第 4 周 | Day 16-20 | MCP 完整协议视角 | 补齐 Tools、Resources、Prompts、Roots 和协议测试 |
| 第 5 周 | Day 21-25 | RAG 基础与高级检索 | 构建可引用、可评测的知识检索链路 |
| 第 6 周 | Day 26-30 | Memory 与上下文压缩 | 管理短期/长期记忆和上下文预算 |
| 第 7 周 | Day 31-35 | Agent 编排 | 实现计划、行动、观察、反思、回答闭环 |
| 第 8 周 | Day 36-40 | 安全、观测、评测、测试 | 建立安全边界、trace、eval 和测试体系 |
| 第 9 周 | Day 41-45 | 部署、性能、扩展验收 | 完成运行态、成本控制和端到端验收 |

### 第 1 周：Agent 基础、Prompt 与工程骨架

- [x] Day 01：理解 AI Agent 与普通 LLM 应用的区别
  - 学习重点：LLM 应用、Tool Calling 应用、Agent 应用的边界
  - 实践产出：写出本项目的 Agent 工作流图
  - 验收标准：能解释为什么排障场景适合 Agent

- [x] Day 02：明确排障 Agent 的 MVP 范围
  - 学习重点：KISS、YAGNI、只读边界、真实项目接入范围
  - 实践产出：定义首个用户问题和预期诊断报告格式
  - 验收标准：MVP 不包含重启、部署、改配置、写数据库等高风险操作

- [x] Day 03：学习 Prompt / Instruction Engineering
  - 学习重点：system instruction、developer instruction、tool instruction、输出格式约束
  - 实践产出：设计排障 Agent 的基础指令和诊断报告格式约束
  - 验收标准：Prompt 中不写死具体业务逻辑，业务能力通过工具和知识库提供

- [x] Day 04：初始化项目结构
  - 学习重点：模块边界、职责划分、Agent 与工具解耦
  - 实践产出：创建 `agent-app`、`mcp-server`、`knowledge-base`、`evals`、`traces`
  - 验收标准：目录结构能支撑 LLM、MCP、RAG、Memory、Evals 后续扩展

- [x] Day 05：配置管理与运行入口设计
  - 学习重点：模型配置、目标项目路径、只读根目录、环境变量和本地 profile
  - 实践产出：定义配置文件结构和 CLI / REST 运行入口草案
  - 验收标准：敏感配置不进入代码仓库，目标项目访问路径可配置

### 第 2 周：LLM API 与 Tool Calling

- [x] Day 06：实现最小 LLM API 调用
  - 学习重点：模型客户端、超时、错误处理、配置隔离
  - 实践产出：`ChatModelClient`
  - 验收标准：能输入问题并得到模型回复

- [x] Day 07：实现结构化输出
  - 学习重点：JSON schema、响应解析、字段校验、失败重试
  - 实践产出：诊断报告结构 `summary / evidence / nextActions / riskLevel`
  - 验收标准：模型输出可以稳定解析为 Java 对象

- [x] Day 08：理解 Tool Calling 的执行模型
  - 学习重点：工具 schema、参数、返回值、失败语义、幂等性
  - 实践产出：设计只读工具接口规范
  - 验收标准：能说明工具调用、结构化输出和普通文本回答的区别

- [x] Day 09：实现本地只读工具 `search_code`
  - 学习重点：代码搜索、输入限制、结果裁剪、路径约束
  - 实践产出：按关键词搜索目标 Java 项目源码
  - 验收标准：输入关键词能返回相关文件路径和片段，且不能越过允许根目录

- [x] Day 10：实现本地只读工具 `git_history` 和 `read_config`
  - 学习重点：Git 查询、配置读取、敏感值脱敏、只读边界
  - 实践产出：查询提交历史、读取配置并脱敏输出
  - 验收标准：不会泄露 token、secret、password，所有工具返回统一 `ToolResult`

### 第 3 周：MCP 基础与工具化

- [x] Day 11：理解 MCP 的角色
  - 学习重点：MCP Server、MCP Client、tool list、tool call、transport
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
  - 学习重点：只读工具、写操作工具、高风险工具、工具元数据
  - 实践产出：工具元数据包含 `riskLevel` 和 `readOnly`
  - 验收标准：只读工具默认允许，高风险工具不会执行

### 第 4 周：MCP 完整协议视角

- [ ] Day 16：学习 MCP Resources
  - 学习重点：Resources 与 Tools 的区别、文件/配置/schema 上下文暴露方式
  - 实践产出：把接口文档、配置摘要或项目元信息作为 MCP Resource 暴露
  - 验收标准：Agent 能读取资源上下文，而不是把所有读取都建模成工具调用

- [ ] Day 17：学习 MCP Prompts
  - 学习重点：Prompt 模板、参数化排障流程、复用工作流
  - 实践产出：定义 `troubleshoot_voice_issue` 这类排障 Prompt 模板
  - 验收标准：常见排障任务能通过 Prompt 模板启动

- [ ] Day 18：学习 MCP Roots 与访问边界
  - 学习重点：允许访问目录、用户同意、路径隔离、越权防护
  - 实践产出：定义目标项目 roots 和拒绝越界访问的策略
  - 验收标准：MCP Server 不能读取允许范围外的文件

- [ ] Day 19：补齐 MCP transport 和协议版本意识
  - 学习重点：stdio、HTTP/SSE 或 Streamable HTTP、协议版本、兼容性
  - 实践产出：记录本项目采用的 MCP transport 和启动方式
  - 验收标准：Agent App 与 MCP Server 的连接方式可复现

- [ ] Day 20：建立 MCP contract test
  - 学习重点：工具列表、参数 schema、错误响应、权限元数据验证
  - 实践产出：为 MCP 工具和资源建立契约测试
  - 验收标准：工具 schema 变更会被测试发现

### 第 5 周：RAG 基础与高级检索

- [ ] Day 21：整理知识库结构
  - 学习重点：文档来源、知识切分、metadata、引用路径
  - 实践产出：`knowledge-base` 下放入接口文档、排障手册、历史案例
  - 验收标准：每份知识都有来源路径和 metadata

- [ ] Day 22：实现最小全文检索
  - 学习重点：先用简单方案打通闭环，控制结果数量和片段长度
  - 实践产出：`retrieve_docs` 支持关键词检索
  - 验收标准：能返回相关文档片段和来源

- [ ] Day 23：实现 query rewrite 与检索意图识别
  - 学习重点：把用户故障描述改写成检索关键词、错误码、模块名
  - 实践产出：为排障问题生成检索查询
  - 验收标准：自然语言问题能转成稳定的检索输入

- [ ] Day 24：评估 embedding、hybrid search 和 rerank
  - 学习重点：全文检索、embedding、向量检索、混合检索、重排的取舍
  - 实践产出：写出是否引入向量库的决策记录
  - 验收标准：不为了技术完整性过早引入复杂依赖

- [ ] Day 25：实现 RAG 引用与评测
  - 学习重点：证据优先、引用来源、防幻觉、召回质量评测
  - 实践产出：RAG 回答包含 `source`，并建立最小 RAG eval case
  - 验收标准：没有来源的结论必须标记为推断

### 第 6 周：Memory 与上下文压缩

- [ ] Day 26：理解 Agent Memory 类型
  - 学习重点：短期记忆、长期记忆、用户偏好、项目事实、历史故障案例
  - 实践产出：设计本项目 Memory 分类表
  - 验收标准：能区分本轮上下文、长期事实和历史经验

- [ ] Day 27：设计短期会话记忆
  - 学习重点：本轮问题、已查工具、关键 observation、未完成假设
  - 实践产出：定义 `SessionMemory`
  - 验收标准：多轮排障时不会重复查询同一证据

- [ ] Day 28：设计长期项目记忆
  - 学习重点：项目事实、模块边界、常见故障、用户偏好、写入条件
  - 实践产出：定义 `ProjectMemory` 和历史故障案例格式
  - 验收标准：长期记忆必须可追溯来源，不把推断当事实写入

- [ ] Day 29：实现工具结果与日志压缩
  - 学习重点：长日志摘要、代码片段压缩、保留关键证据、去噪
  - 实践产出：压缩 `search_logs` / `search_code` 的结果
  - 验收标准：压缩后仍保留时间、路径、错误码、关键堆栈和来源

- [ ] Day 30：实现上下文预算与压缩策略
  - 学习重点：上下文窗口、token budget、丢弃策略、摘要刷新
  - 实践产出：定义 context pack 结构和压缩触发规则
  - 验收标准：长会话下 Agent 能继续工作，且证据链可追溯

### 第 7 周：Agent 编排

- [ ] Day 31：设计 Agent 状态模型
  - 学习重点：Plan、Act、Observe、Reflect、Answer、Memory、Trace
  - 实践产出：定义 Agent 状态对象
  - 验收标准：每一轮执行都能被序列化记录

- [ ] Day 32：实现 Plan 阶段
  - 学习重点：结构化计划、步骤拆分、工具候选、假设声明
  - 实践产出：用户输入故障现象后生成排障计划
  - 验收标准：计划包含要查什么、为什么查、预期证据是什么

- [ ] Day 33：实现 Act 和 Observe 阶段
  - 学习重点：工具选择、参数生成、结果归档、失败恢复
  - 实践产出：Agent 能按计划调用 MCP 工具
  - 验收标准：每次工具调用都有输入、输出、状态和 trace id

- [ ] Day 34：实现 Reflect 阶段
  - 学习重点：是否继续、是否缺证据、是否收敛、是否需要用户澄清
  - 实践产出：Agent 根据 observation 和 memory 决定下一步
  - 验收标准：有最大轮数限制，避免无限循环

- [ ] Day 35：实现最终诊断报告
  - 学习重点：事实、推断、风险、建议、后续验证分离
  - 实践产出：输出完整排障报告
  - 验收标准：报告能区分事实证据、历史经验和模型推断

### 第 8 周：安全、观测、评测与测试

- [ ] Day 36：设计高风险操作审批机制
  - 学习重点：危险操作识别、人工确认、审计记录、审批前后状态
  - 实践产出：写操作工具接口设计，但不执行真实操作
  - 验收标准：重启、部署、改配置必须进入审批流程

- [ ] Day 37：学习 Prompt Injection 与工具输出安全
  - 学习重点：日志/文档/Git diff 是不可信输入，tool output injection 防护
  - 实践产出：为工具结果加不可信输入标记和安全处理规则
  - 验收标准：工具输出中的指令不能覆盖系统指令或触发危险动作

- [ ] Day 38：实现 trace、指标和失败样本记录
  - 学习重点：调用链、latency、token、工具错误、失败样本沉淀
  - 实践产出：每次 Agent 运行写入 `traces`
  - 验收标准：能还原一次完整排障过程

- [ ] Day 39：建立 eval case 和 trace replay
  - 学习重点：输入、期望工具、期望证据、期望结论、回放测试
  - 实践产出：至少 5 个排障评测用例
  - 验收标准：能重复运行并判断 Agent 是否退化

- [ ] Day 40：补齐测试体系
  - 学习重点：工具单测、MCP contract test、RAG eval、Agent golden case
  - 实践产出：为关键模块建立测试分层
  - 验收标准：核心工具、协议契约和诊断报告结构都有自动化验证

### 第 9 周：部署、性能、扩展与端到端验收

- [ ] Day 41：实现 CLI 或 REST 入口
  - 学习重点：用户输入、运行参数、输出格式、本地开发体验
  - 实践产出：可以通过命令行或 REST 调用排障 Agent
  - 验收标准：一次排障任务可以脱离测试代码运行

- [ ] Day 42：部署与运行态设计
  - 学习重点：Docker、health check、配置挂载、日志路径、MCP Server 独立部署
  - 实践产出：写出本地部署和运行说明
  - 验收标准：Agent App 和 MCP Server 的启动、停止、健康检查可复现

- [ ] Day 43：成本、缓存与性能优化
  - 学习重点：prompt caching、tool result cache、embedding cache、超时、重试、限流
  - 实践产出：定义缓存和超时策略
  - 验收标准：重复排障不会无意义重复消耗模型和工具调用

- [ ] Day 44：Human-in-the-loop 与写操作扩展设计
  - 学习重点：人工确认、审批提示、执行前 diff、执行后验证
  - 实践产出：设计 `restart_service`、`update_config`、`trigger_deploy` 的审批流程
  - 验收标准：高风险操作只进入设计，不在第一阶段真实执行

- [ ] Day 45：完整端到端验收与复盘
  - 学习重点：从用户问题到诊断报告的完整闭环，以及后续框架对照学习
  - 实践产出：运行真实故障问题排障流程并写复盘
  - 验收标准：LLM API、Prompt、Tool Calling、MCP、RAG、Memory、上下文压缩、编排、安全、trace、eval、测试全部串联

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
| 2026-06-23 | Day 01：理解 AI Agent 与普通 LLM 应用的区别 | 已完成 | 明确普通 LLM 应用、Tool Calling 应用和 Agent 应用的边界，确认排障 Agent 应以多步证据收集、观察和收敛为核心，并坚持第一阶段只读边界。 |
| 2026-06-23 | Day 02：明确排障 Agent 的 MVP 范围 | 已完成 | 明确第一版 MVP 只做 Java 后端接口异常的只读诊断，定义首个用户问题、只读证据源、排除的高风险操作和诊断报告格式。 |
| 2026-06-23 | Day 03：学习 Prompt / Instruction Engineering | 已完成 | 明确 system instruction、developer instruction、tool instruction 的职责分工，确认 Prompt 负责行为契约和输出格式约束，不承载业务事实，业务能力应通过工具和知识库提供。 |
| 2026-06-23 | Day 04：初始化项目结构 | 已完成 | 创建 `agent-app`、`mcp-server`、`knowledge-base`、`evals`、`traces` 五个核心模块，明确 Agent 编排、MCP 能力、知识库、评测和 trace 的职责边界，并确认 trace 脱敏与 eval 契约测试价值。 |
| 2026-06-23 | Day 05：配置管理与运行入口设计 | 已完成 | 明确 DeepSeek 模型配置、目标项目路径、只读根目录、环境变量和 CLI / REST 入口草案，确认敏感配置不进入仓库，入口复用同一套配置对象，并保持只读访问边界。 |
| 2026-06-23 | Day 06：实现最小 LLM API 调用 | 已完成 | 实现 `ChatModelClient` 模型调用边界和 OpenAI-compatible 非流式客户端，明确模型供应商可替换、模型失败应作为 Agent 可识别的受控失败处理，并坚持先打通普通文本回复，结构化诊断报告留到 Day 07。 |
| 2026-06-23 | Day 07：实现结构化输出 | 已完成 | 实现 `DiagnosticReport` 结构化报告契约、JSON 解析和字段校验，明确结构化输出只能保证形状可解析，真实结论仍需依赖工具、RAG 来源和 trace 证据校验。 |
| 2026-06-23 | Day 08：理解 Tool Calling 的执行模型 | 已完成 | 建立 `TroubleshootingTool`、`ToolDefinition`、`ToolCall`、`ToolResult` 等只读工具调用契约，明确模型负责选工具、生成参数和消费 observation，程序负责校验、权限、执行和结构化返回，并理解失败 observation 与幂等只读工具边界。 |
| 2026-06-23 | Day 09：实现本地只读工具 `search_code` | 已完成 | 实现 `LocalCodeSearchTool` 本地只读源码搜索工具，明确关键词输入限制、允许根目录、符号链接越权风险、结果裁剪和“未找到” observation 的边界，并通过单元测试覆盖正常搜索、参数错误、结果上限和路径越界防护。 |
| 2026-06-23 | Day 10：实现本地只读工具 `git_history` 和 `read_config` | 已完成 | 实现 `LocalGitHistoryTool` 和 `LocalConfigReadTool`，明确 Git 历史与配置读取都必须受 allowedRoot 约束，敏感值要在进入模型上下文前脱敏，并通过测试覆盖提交查询、配置读取、路径越界、符号链接和绝对路径防护。 |
| 2026-06-23 | Day 11：理解 MCP 的角色 | 已完成 | 明确 MCP Host、MCP Client、MCP Server、`tools/list`、`tools/call` 和 transport 的职责边界，确认 `agent-app` 后续通过 MCP Client 连接 `mcp-server`，工具能力不再长期直接写进 Agent，Day 12 本地最小闭环优先采用 stdio。 |

## 当前进度

- 总节点数：45
- 已学习：11
- 当前建议节点：Day 12

## 后续标记方式

你可以直接说：

- `Day 01 学完了`
- `把 Day 03 标记为已学习`
- `今天学到 Tool Calling 了，帮我更新进度`
- `今天学到 Memory 了，帮我更新进度`

我会更新本文档中的 checkbox、学习记录和当前进度。
