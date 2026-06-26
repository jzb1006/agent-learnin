# AGENTS.md

## 项目定位

这是一个面向 Java 后端开发者的企业级 AI Agent 工程项目。

当前主线已经从“每日学习路线 + MCP 排障 Agent”切换为：

```text
企业级智能客服与订单协同 Agent 平台
```

业务参考优先使用：

```text
<AI_ENGINEER_TRAINING_ROOT>/week10
```

目标不是快速生成 Demo，而是用 Spring Boot / Spring AI 逐步落地一套可维护、可观测、可扩展的企业级业务 Agent 平台。

## 新窗口启动规则

每次新窗口开始工作前，AI 必须先阅读：

1. `README.md`
2. `docs/ENTERPRISE_CUSTOMER_SERVICE_AGENT_30_DAY_PLAN.md`
3. `docs/week10-enterprise-customer-service-agent-blueprint.md`
4. `docs/enterprise-spring-boot-ai-agent-ecosystem.md`

如果 `.local/project-memory.md` 存在，AI 还必须读取它，用于解析本机参考路径、远程开发服务器和 SSH 连接占位符。该文件是本地私有记忆，已被 `.gitignore` 忽略，禁止提交、推送或把其中真实值复制到公开文档。

如需参考原始业务样例，再读取：

1. `<AI_ENGINEER_TRAINING_ROOT>/week10/README.md`
2. `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/architecture_design.md`
3. `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/api_specification.md`
4. `<AI_ENGINEER_TRAINING_ROOT>/week10/work_v3/app.py`
5. `<AI_ENGINEER_TRAINING_ROOT>/week10/work_v3/graph.py`
6. `<AI_ENGINEER_TRAINING_ROOT>/week10/work_v3/tools.py`
7. `<AI_ENGINEER_TRAINING_ROOT>/week10/work_v3/mcp_server.py`

旧每日学习路线已经归档到分支：

```text
codex/archive-daily-learning-route
```

主分支不再按 `Day XX` 自动推进。

如果用户说“开始 Day XX”，默认按 `docs/ENTERPRISE_CUSTOMER_SERVICE_AGENT_30_DAY_PLAN.md` 中新的 30 天计划推进，而不是旧每日学习路线。

## 项目目标

新主线要逐步实现：

- 智能客服对话
- 意图识别与路由
- 订单查询
- FAQ / 政策 / 产品知识 RAG
- 知识库管理
- MCP Tools / Resources / Prompts
- 多租户隔离
- Memory / Context Compression
- Agent 编排
- 多 Agent 扩展
- 高风险操作审批
- 安全脱敏与 Prompt Injection 防护
- trace、metrics、eval、日志审计
- Docker Compose / 生产化部署

## 默认技术路线

主线技术栈：

- Spring Boot
- Spring AI
- Spring AI MCP / MCP Java SDK
- Spring Security
- Spring Data JDBC / JPA
- PostgreSQL / pgvector
- Redis
- Vite / React / TypeScript / Ant Design
- Micrometer / OpenTelemetry
- Prometheus / Grafana
- Docker / Docker Compose

对照框架：

- Spring AI Alibaba
- LangChain4j
- Google ADK Java

对照框架只用于理解差异和借鉴设计，不应在 MVP 阶段同时引入多个 Agent 框架。

## 版本基线与选型纪律

截至 2026-06-26，后续创建工程、补依赖、写部署脚本时默认遵循以下版本基线：

| 层级 | 默认选择 | 约束 |
| --- | --- | --- |
| JDK | Java 21 LTS 起步，预留 Java 25 LTS 升级 | 企业落地优先稳定；若 Spring Boot 4.1.x、构建插件和 IDE 全部兼容，可直接使用 Java 25。 |
| Spring Boot | 4.1.x 优先，3.5.x 保守回退 | 不使用 Spring Boot 2.x；不主动引入已停止维护的 starter。 |
| Spring AI | 2.0.x | 使用 Spring AI BOM 管理 AI 相关依赖。 |
| MCP | Spring AI MCP / MCP Java SDK | 通过 Spring AI BOM 管理 MCP 版本，不手工覆盖；独立使用 MCP SDK 时必须重新核对兼容矩阵。 |
| 数据库 | PostgreSQL 18.x + pgvector 0.8.x | 向量检索优先 pgvector，不自研向量索引。 |
| Redis | Redis 8.x | 只用于缓存、会话、Memory、限流等明确场景。 |
| 前端 | Node.js 24 LTS + Vite 8.x + React 19.x + TypeScript 6.x | 不使用 Create React App；如生态兼容不足，TypeScript 回退 5.9.x。 |
| UI | Ant Design 6.x + TanStack Query v5 | Ant Design 5.x 只作为兼容兜底，第一版优先使用 6.x。 |
| 观测 | Actuator + Micrometer + OpenTelemetry + Prometheus + Grafana | Web 调试台不替代 Grafana。 |
| 部署 | Docker Compose v2 | 学习阶段先 Compose，Kubernetes 作为后续扩展。 |

执行规则：

- MVP 只引入主线依赖：Spring Boot、Spring AI、Spring AI MCP、PostgreSQL、Redis、Vite / React。
- Spring AI Alibaba、LangChain4j、Google ADK Java 只作为对照或后续扩展，不在第一版同时落地。
- 版本文档只写 major/minor 基线；具体 patch 在创建工程当天通过官方源或包管理器确认。
- 发现依赖兼容冲突时，优先保持主线闭环可运行，再记录升级原因和回退条件。

## 推荐项目结构

主项目建议落在：

```text
projects/enterprise-customer-service-agent/
  customer-agent-app/
  customer-mcp-server/
  customer-domain/
  customer-admin-web/
  knowledge-base/
  evals/
  traces/
  deploy/
```

旧排障 Agent 项目已从主分支移除，并保留在 `codex/archive-daily-learning-route` 归档分支中。

## Web 调试台边界

`customer-admin-web` 是 P0 的本地 Agent 调试台，不是后期才做的完整管理后台。

固定技术栈：

```text
Vite
React
TypeScript
Ant Design
TanStack Query
```

第一版必须围绕开发调试体验：

- 对话测试
- 结构化响应查看
- 工具调用链查看
- RAG 来源查看
- 订单接口调试
- 审批流程模拟
- health / runtime 状态查看

不要在 MVP 阶段扩展成完整运营后台、权限后台、租户管理后台或 BI 面板。

## 监控台边界

运行态监控台使用 Prometheus + Grafana，不在 `customer-admin-web` 内自研完整 dashboard。

职责边界：

| 界面 | 职责 |
| --- | --- |
| `customer-admin-web` | 面向开发调试，查看 Agent 决策、工具调用、RAG 来源、审批模拟 |
| Grafana Dashboard | 面向运行态监控，查看 metrics、latency、error rate、工具失败率、RAG 命中率、资源状态 |

第一版 Grafana Dashboard 至少覆盖：

- API 请求量、P95/P99 延迟、错误率。
- LLM 调用次数、耗时、失败率、token 消耗。
- 工具调用次数、失败率、耗时。
- RAG 检索次数、命中率、检索耗时。
- 审批请求数和拒绝率。
- PostgreSQL、Redis、JVM、连接池和线程池指标。

`customer-admin-web` 可以提供 Grafana 链接或关键指标摘要，但不要复制 Grafana 的完整监控能力。

## 默认部署环境

项目部署和中间件部署默认落在同一台服务器：

```text
主机：<DEV_SERVER_HOST>
用户：<DEV_SERVER_USER>
本地 SSH 证书：<SSH_IDENTITY_FILE>
连接示例：ssh -i <SSH_IDENTITY_FILE> <DEV_SERVER_USER>@<DEV_SERVER_HOST>
```

默认纳入部署设计的组件：

- Spring Boot Agent 服务
- MCP Server
- PostgreSQL / pgvector
- Redis
- Prometheus
- Grafana
- OpenTelemetry Collector（如后续启用）

远程部署、远程容器重启、数据库结构变更、中间件配置修改、生产 API 调用都必须视为高风险操作。没有用户明确确认时，只允许写部署文档、生成配置草案或执行只读检查。

数据库变更规则：

- 不使用 Flyway / Liquibase 等数据库迁移框架。
- 不使用 Hibernate `ddl-auto=update` 自动改表；开发期也应使用 `validate` 或 `none`。
- DDL 必须写入仓库中的 SQL 脚本，经过确认后人工执行。
- 执行远程 DDL 前必须说明目标库、脚本、影响表、回滚或补救方式。
- 执行后必须记录脚本名、执行时间、执行结果和验证命令。

## 工程边界

第一阶段只做低风险业务闭环：

允许：

- 查询订单
- 查询 FAQ / 政策 / 产品知识
- 生成客服回复
- 记录 trace
- 记录未命中问题
- 生成人工转接建议

禁止默认执行：

- 真实退款
- 真实取消订单
- 真实改签
- 写生产数据库
- 调生产 API
- 删除知识库或订单数据
- 绕过人工审批执行高风险工具

高风险动作必须显式说明影响范围，并等待用户确认。

## 阶段推进规则

每个阶段都必须包含：

1. 目标
2. 业务场景
3. 模块边界
4. 接口设计
5. 数据模型
6. 安全边界
7. 验证方式
8. 测试用例

不要只做概念学习。每个知识点都要落到企业客服订单平台中。

## 参考迁移原则

从 `ai-engineer-training/week10` 迁移时：

- 参考业务能力和架构，不直接照搬 Python 代码。
- FastAPI API 设计映射为 Spring MVC / WebFlux Controller。
- LangGraph 路由映射为 Java 编排服务或 Spring AI Advisor / workflow。
- FAISS 映射为 Spring AI VectorStore，优先 pgvector。
- SQLite 订单库映射为 PostgreSQL。
- Python MCP Server 映射为 Spring AI MCP / MCP Java SDK。
- RedactionMiddleware 映射为 Spring Web Filter / HandlerInterceptor。
- `/health` 指标映射为 Spring Boot Actuator + Micrometer。

## 代码与文档原则

- KISS：先实现一个客服订单最小闭环。
- YAGNI：不要提前实现复杂多 Agent、全量后台、真实支付退款。
- DRY：模型调用、工具调用、权限检查、脱敏、trace 必须统一封装。
- SOLID：业务服务、Agent 编排、工具实现、MCP 暴露、安全治理必须分层。

所有代码变更都必须能解释：

- 为什么现在需要
- 放在哪一层
- 如何验证
- 失败时如何定位

## 默认响应语言

默认使用简体中文。

回答要结论先行，解释清楚但避免冗余。
