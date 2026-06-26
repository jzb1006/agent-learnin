# 企业级智能客服与订单 Agent 平台

目标：以 `ai-engineer-training/week10` 的可落地智能客服系统为业务参考，用 Java / Spring Boot / Spring AI 重建一个企业级 AI Agent 项目。

当前主线不再继续“每日学习路线 + 排障 Agent”模式。旧路线已保存在分支：

```text
codex/archive-daily-learning-route
```

## 新主线定位

项目方向：

```text
Enterprise AI Customer Service & Order Agent Platform
企业级智能客服与订单协同 Agent 平台
```

业务目标：

- 智能客服对话
- FAQ / 政策 / 产品知识 RAG
- 订单查询
- 退款、取消、改签等高风险操作审批
- 人工转接
- 多租户知识库与订单数据隔离
- MCP 工具暴露
- 多 Agent 协作扩展
- 安全脱敏、审计、评测、监控、部署

## 核心参考

本项目优先参考本机资料，真实本机根路径见 `.local/project-memory.md`：

- `<AI_ENGINEER_TRAINING_ROOT>/week10/README.md`
- `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/architecture_design.md`
- `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/1智能客服系统产品需求文档.md`
- `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/api_specification.md`
- `<AI_ENGINEER_TRAINING_ROOT>/week10/docs/deployment_guide.md`
- `<AI_ENGINEER_TRAINING_ROOT>/week10/work_v3/app.py`

对应 Java 化蓝图：

- [30 天企业级智能客服订单 Agent 学习与落地计划](docs/ENTERPRISE_CUSTOMER_SERVICE_AGENT_30_DAY_PLAN.md)
- [Week10 企业客服订单 Agent Java 化蓝图](docs/week10-enterprise-customer-service-agent-blueprint.md)
- [企业级 Spring Boot AI Agent 生态落地路线](docs/enterprise-spring-boot-ai-agent-ecosystem.md)

## 推荐技术栈

```text
Spring Boot
Spring AI
Spring AI MCP / MCP Java SDK
Spring Security
Spring Data JDBC / JPA
PostgreSQL / pgvector
Redis
Vite / React / TypeScript / Ant Design
Micrometer / OpenTelemetry
Prometheus / Grafana
Docker / Docker Compose
```

## 版本基线

截至 2026-06-26，本项目主线技术栈属于行业通用、仍在活跃演进的企业级 Java AI 应用栈。Day 02 创建工程时按以下基线落地：

| 层级 | 推荐基线 | 说明 |
| --- | --- | --- |
| JDK | Java 21 LTS 起步，预留 Java 25 LTS 升级 | Java 25 已是最新 LTS，但 Java 21 仍是企业 Spring Boot 项目常见稳妥选择。 |
| 后端框架 | Spring Boot 4.1.x；如依赖兼容性不足，回退 3.5.x | 新项目优先看 4.1.x；遇到 Spring AI、第三方 starter 或 IDE 兼容问题时使用 3.5.x。 |
| AI 框架 | Spring AI 2.0.x | 主线使用 Spring AI，不在 MVP 同时引入多套 Agent 框架。 |
| MCP | Spring AI MCP，版本由 Spring AI BOM 管理 | 不手工覆盖 MCP Java SDK 版本；独立 SDK 只在脱离 Spring AI 时单独评估。 |
| 数据库 | PostgreSQL 18.x + pgvector 0.8.x | 订单、知识库元数据、向量检索统一优先落 PostgreSQL。 |
| 缓存 / 会话 | Redis 8.x | 用于会话、短期 Memory、限流和调试态缓存。 |
| 前端 | Node.js 24 LTS + Vite 8.x + React 19.x + TypeScript 6.x | Web 调试台使用现代前端栈，不采用 Create React App；如生态兼容不足，TypeScript 回退 5.9.x。 |
| UI / 数据请求 | Ant Design 6.x + TanStack Query v5 | Ant Design 5.x 只作为兼容兜底，第一版优先使用 6.x。 |
| 观测 | Spring Boot Actuator + Micrometer + OpenTelemetry | 应用侧先暴露指标和 trace，再接 Prometheus / Grafana。 |
| 监控 | Prometheus 3.x + Grafana 13.x | Grafana 负责运行态 dashboard，Web 调试台不重复实现完整监控台。 |
| 部署 | Docker Compose v2 | 学习阶段优先 Compose，Kubernetes 作为后续扩展。 |

选型原则：

- **主线依赖只保留一套：** Spring Boot + Spring AI + Spring AI MCP。
- **对照框架不进入 MVP：** Spring AI Alibaba、LangChain4j、Google ADK Java 只用于学习差异和后续扩展。
- **版本不写死到补丁号：** 创建工程当天再锁定具体 patch，避免课程文档很快过期。
- **避免落后技术：** 不使用 Spring Boot 2.x、Java 8/11、新项目 Create React App、手写向量库、纯日志式观测。

## 默认部署环境

后续项目服务和企业级中间件默认部署到同一台学习服务器：

```text
主机：<DEV_SERVER_HOST>
用户：<DEV_SERVER_USER>
本地 SSH 证书：<SSH_IDENTITY_FILE>
连接示例：ssh -i <SSH_IDENTITY_FILE> <DEV_SERVER_USER>@<DEV_SERVER_HOST>
```

默认部署范围：

- `customer-agent-app`
- `customer-mcp-server`
- PostgreSQL / pgvector
- Redis
- Prometheus
- Grafana
- OpenTelemetry Collector（如后续启用）

远程部署、容器重启、数据库结构变更和中间件配置变更都属于高风险操作，必须先明确影响范围并获得确认后再执行。

数据库变更约定：

- 不引入 Flyway / Liquibase 等自动迁移框架。
- 不允许 Spring Boot 启动时自动改表。
- DDL 以仓库内 SQL 脚本形式维护，人工确认后再连接远程数据库执行。
- 每次 DDL 执行后需要记录脚本名、目标库、执行时间和结果。

对照参考：

- Spring AI Alibaba：多 Agent / workflow / human-in-the-loop
- LangChain4j：Java RAG / tools / memory 对照
- Google ADK Java：Agent-first / 多 Agent 对照

## 目标项目结构

后续主项目建议落在：

```text
projects/enterprise-customer-service-agent/
  customer-agent-app/       # 对话入口、Agent 编排、REST API
  customer-mcp-server/      # MCP tools/resources/prompts
  customer-domain/          # 订单、知识库、租户、审批等领域模型
  customer-admin-web/       # 本地 Agent 调试台，Vite + React + TypeScript + Ant Design
  knowledge-base/           # FAQ、政策、产品文档、样例知识库
  evals/                    # 评测集、回归用例、坏例样本
  traces/                   # 本地 trace、工具调用链、审计样本
  deploy/                   # Docker Compose、Prometheus、Grafana
```

## 本地 Web 调试台

`customer-admin-web` 是主线 P0 能力，不是后期完整管理后台。

前端技术栈固定为：

```text
Vite
React
TypeScript
Ant Design
TanStack Query
```

第一阶段只服务本地调试：

- Chat Console：调试 `/chat`。
- Request Inspector：查看 route、riskLevel、nextActions、traceId。
- Tool Calls：查看工具名、参数、耗时、结果。
- RAG Sources：查看知识库命中来源。
- Order Debug：调试订单查询接口。
- Approval Debug：模拟退款、取消、改签审批。
- Health：查看后端、数据库、Redis、模型配置连通性。

## 运行态监控台

监控台使用 Prometheus + Grafana，不放进 `customer-admin-web` 自研。

两类界面职责分开：

| 界面 | 职责 |
| --- | --- |
| `customer-admin-web` | 本地调试 Agent 行为、工具调用、RAG 来源、审批流程 |
| Grafana Dashboard | 监控服务运行态、请求量、延迟、错误率、工具失败率、RAG 命中率、token 成本 |

第一版 Grafana Dashboard 关注：

- API 请求量、P95/P99 延迟、错误率。
- LLM 调用次数、耗时、失败率、token 消耗。
- Tool Calling 次数、失败率、耗时。
- RAG 检索次数、命中率、检索耗时。
- 审批请求数、拒绝率。
- PostgreSQL、Redis、JVM、线程池和连接池指标。

## 30 天阶段路线

1. **MVP 对话与订单查询**
   - `/chat`
   - 意图识别
   - 订单查询工具
   - 结构化客服回复
   - Vite 本地调试台

2. **RAG 知识库**
   - FAQ / 政策文档导入
   - 向量检索
   - 引用来源
   - 知识库增删改

3. **MCP 工具化**
   - `kb_search`
   - `order_lookup`
   - `handoff_to_human`
   - `refund_policy_check`

4. **多租户与安全**
   - `X-Tenant-ID`
   - 租户级知识库与订单库
   - 敏感信息脱敏
   - 工具权限分级

5. **Agent 编排与人工审批**
   - 退款 / 取消 / 改签审批
   - human-in-the-loop
   - 最大循环次数
   - 失败重试

6. **观测、评测与部署**
   - trace
   - metrics
   - eval cases
   - Grafana Dashboard
   - Prometheus / Grafana
   - Docker Compose

详细每日安排见：

```text
docs/ENTERPRISE_CUSTOMER_SERVICE_AGENT_30_DAY_PLAN.md
```

## 旧路线说明

旧每日学习路线和排障 Agent 已从主分支移除，并保留在归档分支中。

后续如果需要查看或恢复旧路线，请切换到：

```bash
git switch codex/archive-daily-learning-route
```
