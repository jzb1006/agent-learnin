# 30 天企业级智能客服订单 Agent 学习与落地计划

## 定位

这是一条面向有多年 Java 开发经验的学习路线。

目标不是补 Java 基础，也不是做一次性 Demo，而是用 30 天完成一个企业级 AI Agent 项目的可落地闭环：

```text
Spring Boot + Spring AI + Tool Calling + RAG + MCP + Memory + 安全审批 + 观测评测 + 部署
```

业务主线：

```text
企业级智能客服与订单协同 Agent 平台
```

核心参考：

```text
<AI_ENGINEER_TRAINING_ROOT>/week10
```

## 技术栈版本基线

截至 2026-06-26，这套课程使用的是当前行业通用的企业级 Java AI 应用栈，不属于落后路线。

| 技术域 | 课程基线 | 为什么这样选 |
| --- | --- | --- |
| Java | Java 21 LTS 起步，预留 Java 25 LTS | Java 25 是最新 LTS；Java 21 仍是企业项目更稳的起步版本。 |
| Spring Boot | 4.1.x 优先，3.5.x 保守回退 | 新项目优先现代基线；遇到 AI / MCP 依赖兼容问题时回退到更稳版本。 |
| Spring AI | 2.0.x | 覆盖 ChatClient、Tool Calling、RAG、MCP、Observability 等主线能力。 |
| MCP | Spring AI MCP，MCP Java SDK 由 BOM 管理 | 避免手工覆盖 SDK 造成兼容问题。 |
| 数据库 | PostgreSQL 18.x + pgvector 0.8.x | 兼顾业务数据、向量检索、事务和审计。 |
| Redis | Redis 8.x | 用于短期 Memory、Session、限流和缓存。 |
| 前端 | Node.js 24 LTS + Vite 8.x + React 19.x + TypeScript 6.x | 面向本地调试台的主流现代前端栈；如生态兼容不足，TypeScript 回退 5.9.x。 |
| UI / 请求 | Ant Design 6.x + TanStack Query v5 | 企业中后台 UI 和服务端状态管理的成熟组合；Ant Design 5.x 只作为兼容兜底。 |
| 观测 | Actuator + Micrometer + OpenTelemetry + Prometheus + Grafana | 对齐企业运行态监控、trace 和指标体系。 |
| 部署 | Docker Compose v2 | 适合学习阶段快速迭代；后续可扩展到 Kubernetes。 |

版本使用规则：

- Day 02 创建工程时再锁定具体 patch 版本。
- 若 Spring Boot 4.1.x 与 Spring AI 2.0.x 或 MCP starter 出现兼容问题，优先回退 Spring Boot 3.5.x，保证学习主线可运行。
- 不使用 Spring Boot 2.x、Java 8/11、新项目 Create React App、Hibernate `ddl-auto=update` 自动改表、手写向量库。
- Spring AI Alibaba、LangChain4j、Google ADK Java 只作为对照框架，不作为 MVP 主依赖。

## 总体节奏

```text
30 天
= 6 个阶段
= 每阶段 5 天
= 每阶段都有可运行产物
```

## 部署环境约定

涉及项目部署和中间件部署时，默认目标服务器为：

```text
主机：<DEV_SERVER_HOST>
用户：<DEV_SERVER_USER>
本地 SSH 证书：<SSH_IDENTITY_FILE>
连接示例：ssh -i <SSH_IDENTITY_FILE> <DEV_SERVER_USER>@<DEV_SERVER_HOST>
```

默认部署组件：

- `customer-agent-app`
- `customer-mcp-server`
- PostgreSQL / pgvector
- Redis
- Prometheus
- Grafana
- OpenTelemetry Collector（如后续启用）

执行边界：

- Day 18 可以先产出本地 Docker Compose 和初始化脚本，不默认远程安装数据库。
- Day 28 可以先产出观测配置，不默认修改远程 Prometheus / Grafana。
- Day 30 可以准备远程部署脚本和验收清单，但真正连接服务器、上传文件、重启容器、执行 DDL 前必须再次确认影响范围。

数据库变更约定：

- 不使用 Flyway / Liquibase。
- 不让应用启动时自动执行 DDL。
- 表结构变更统一通过仓库内 SQL 脚本维护，再由人工确认后执行。
- SQL 脚本建议放在 `projects/enterprise-customer-service-agent/deploy/sql/`。
- 每次执行远程 DDL 前，必须明确目标库、脚本名、影响范围和验证方式。

每个 Day 必须输出至少一种产物：

- 可运行代码
- API
- 测试用例
- 架构图
- 设计文档
- eval case
- 部署配置
- trace / metrics 样例

## 阶段总览

| 阶段 | 天数 | 主题 | 阶段产物 |
| --- | --- | --- | --- |
| 阶段 1 | Day 01-05 | 项目建模与 Spring Boot / Vite 骨架 | 多模块工程 + 基础 API + 本地调试台 |
| 阶段 2 | Day 06-10 | LLM 调用、Prompt 与结构化输出 | 客服回复模型 + 意图识别 + Web 响应查看 |
| 阶段 3 | Day 11-15 | Tool Calling 与订单业务工具 | 订单/人工转接/政策工具 + Tool Calls 面板 |
| 阶段 4 | Day 16-20 | RAG 知识库与多租户 | FAQ/政策检索 + 租户隔离 + RAG Sources 面板 |
| 阶段 5 | Day 21-25 | MCP、Memory、安全与审批 | MCP Server/Client + 审批边界 + 审批调试 |
| 阶段 6 | Day 26-30 | Agent 编排、观测、评测与运行手册 | 可观测、可评测、可本地调试系统 |

## 当前进度

| Day | 状态 | 完成日期 | 产物 |
| --- | --- | --- | --- |
| Day 01 | 已完成 | 2026-06-26 | [从 Week10 提炼 Java 项目范围](day-01-week10-java-scope.md) |
| Day 02 | 已完成 | 2026-06-27 | [创建多模块项目骨架](day-02-project-skeleton.md)，[主工程目录](../projects/enterprise-customer-service-agent/README.md) |
| Day 03 | 已完成 | 2026-06-27 | [定义核心领域模型](day-03-core-domain-model.md)，[customer-domain](../projects/enterprise-customer-service-agent/customer-domain/README.md) |
| Day 04 | 已完成 | 2026-06-27 | [实现基础 REST API](day-04-basic-rest-api.md)，[customer-agent-app](../projects/enterprise-customer-service-agent/customer-agent-app/README.md)，[customer-admin-web](../projects/enterprise-customer-service-agent/customer-admin-web/README.md) |
| Day 05 | 已完成 | 2026-06-27 | [配置、错误处理与基础测试](day-05-configuration-error-handling-testing.md) |
| Day 06 | 已完成 | 2026-06-27 | [接入 Spring AI ChatClient](day-06-spring-ai-chat-client.md) |
| Day 07 | 已完成 | 2026-06-27 | [设计客服 Agent Prompt](day-07-customer-agent-prompt.md) |
| Day 08 | 已完成 | 2026-06-27 | [实现意图识别](day-08-intent-routing.md) |
| Day 09 | 已完成 | 2026-06-27 | [实现结构化客服回复](day-09-structured-agent-response.md) |
| Day 10 | 已完成 | 2026-06-29 | [阶段 2 集成验证](day-10-stage2-integration-validation.md) |
| Day 11 | 已完成 | 2026-06-29 | [定义工具契约](day-11-tool-contract.md) |
| Day 12 | 已完成 | 2026-06-29 | [实现订单查询工具](day-12-order-lookup-tool.md) |
| Day 13 | 已完成 | 2026-06-29 | [实现知识目录与人工转接工具](day-13-course-catalog-handoff-tools.md) |
| Day 14 | 已完成 | 2026-06-29 | [实现退款政策检查工具](day-14-refund-policy-check-tool.md) |
| Day 15 | 已完成 | 2026-06-29 | [Tool Calling 集成](day-15-tool-calling-integration.md) |
| Day 16 | 已完成 | 2026-06-29 | [整理知识库结构](day-16-knowledge-base-structure.md) |
| Day 17 | 已完成 | 2026-06-29 | [接入 Spring AI RAG](day-17-spring-ai-rag.md) |
| Day 18 | 已完成 | 2026-06-30 | [引入 pgvector](day-18-pgvector.md) |
| Day 19 | 已完成 | 2026-06-30 | [多租户隔离](day-19-multi-tenant-isolation.md) |

## 阶段 1：项目建模与 Spring Boot 骨架

### Day 01：从 Week10 提炼 Java 项目范围

目标：

- 读取 Week10 的 PRD、架构、API、部署文档。
- 明确 Java 版项目的 MVP 边界。

输入参考：

- `ai-engineer-training/week10/docs/1智能客服系统产品需求文档.md`
- `ai-engineer-training/week10/docs/architecture_design.md`
- `ai-engineer-training/week10/docs/api_specification.md`

产物：

- `docs/day-01-week10-java-scope.md`
- 业务流程 Mermaid 图

验收：

- 能说明为什么主线选择“智能客服订单平台”。
- 能说明 MVP 不包含真实退款、真实取消、真实改签。

### Day 02：创建多模块项目骨架

目标：

- 创建 Java 主项目结构。
- 创建本地 Web 调试台骨架。

建议目录：

```text
projects/enterprise-customer-service-agent/
  customer-agent-app/
  customer-domain/
  customer-mcp-server/
  customer-admin-web/
  knowledge-base/
  evals/
  traces/
  deploy/
```

产物：

- Maven 或 Gradle 多模块工程
- `customer-admin-web` Vite 工程
- 根 README
- 模块 README

验收：

- 项目可被 IDE 正确识别。
- 空测试能跑通。
- Vite 调试台能本地启动并显示基础页面。

### Day 03：定义核心领域模型

目标：

- 先建领域边界，再写 Agent。

核心模型：

- Tenant
- CustomerOrder
- KnowledgeItem
- ConversationTrace
- ApprovalRequest
- ToolRiskLevel

产物：

- `customer-domain`
- 领域模型单元测试

验收：

- 订单、租户、知识库、审批、trace 的职责边界清楚。

### Day 04：实现基础 REST API

目标：

- 建立最小服务入口。
- 让 Web 调试台能调用基础 API。

接口：

```text
GET /health
POST /chat
GET /api/orders/{orderId}
```

产物：

- `customer-agent-app`
- Controller / Service 分层
- mock 订单数据
- `customer-admin-web` 基础 API 调用页面

验收：

- `GET /health` 返回服务状态。
- `GET /api/orders/{orderId}` 返回 mock 订单。
- `POST /chat` 返回基础结构化响应。
- Web 调试台能展示 health、订单查询和 chat 响应。

### Day 05：配置、错误处理与基础测试

目标：

- 补齐工程基础设施。

产物：

- `application.yml`
- 配置属性类
- 全局异常处理
- API 测试

验收：

- 敏感配置不进仓库。
- 错误响应结构统一。
- 阶段 1 测试全部通过。

## 阶段 2：LLM 调用、Prompt 与结构化输出

### Day 06：接入 Spring AI ChatClient

目标：

- 接通模型调用。

产物：

- ChatClient 配置
- DeepSeek / OpenAI-compatible 配置
- 最小 smoke test

验收：

- 能发送用户问题并得到模型回复。
- 超时和异常能被包装为业务错误。

### Day 07：设计客服 Agent Prompt

目标：

- 把客服边界写成行为契约。

Prompt 约束：

- 不编造订单状态。
- 不承诺真实退款成功。
- 必须引用工具或知识库证据。
- 高风险动作必须进入审批。

产物：

- Prompt 模板
- Prompt 版本号
- Prompt 测试样例

验收：

- Prompt 中不写死具体业务事实。
- 业务事实来自工具、RAG 或用户输入。

### Day 08：实现意图识别

目标：

- 对用户问题做路由。

意图：

- `KNOWLEDGE_QA`
- `ORDER_LOOKUP`
- `REFUND_OR_CANCEL`
- `HUMAN_HANDOFF`
- `DIRECT`

产物：

- IntentRouter
- 结构化输出对象
- fallback 规则

验收：

- 典型客服问题能路由正确。
- 模型失败时有关键词 fallback。

### Day 09：实现结构化客服回复

目标：

- 统一 Agent 输出格式。

响应字段：

- route
- answer
- sources
- riskLevel
- nextActions
- traceId

产物：

- CustomerAgentResponse
- 响应解析与校验
- 单元测试

验收：

- 模型输出能稳定转成 Java 对象。
- 字段缺失时能报出可定位错误。

### Day 10：阶段 2 集成验证

目标：

- 串起 `/chat -> intent -> LLM -> response`。
- 在 Web 调试台展示结构化 Agent 响应。

产物：

- 集成测试
- 阶段复盘文档
- Chat Console
- Request Inspector

验收：

- 课程咨询、订单查询、人工转接、退款请求都能进入正确 route。
- Web 调试台能展示 route、riskLevel、nextActions、traceId。

## 阶段 3：Tool Calling 与订单业务工具

### Day 11：定义工具契约

目标：

- 建立统一工具模型。

产物：

- ToolDefinition
- ToolResult
- ToolRiskLevel
- ToolPermission

验收：

- 所有工具都有名称、描述、参数 schema、风险等级。

### Day 12：实现订单查询工具

目标：

- 让 Agent 查询订单。

工具：

```text
order_lookup(orderId, tenantId)
```

产物：

- OrderLookupTool
- mock repository
- 工具测试

验收：

- 订单不存在时有明确失败语义。
- 不能跨租户查订单。

### Day 13：实现知识目录与人工转接工具

目标：

- 补齐客服基本动作。

工具：

- course_catalog
- handoff_to_human

产物：

- 工具实现
- trace 记录

验收：

- 人工转接只创建记录，不执行外部真实派单。

### Day 14：实现退款政策检查工具

目标：

- 支持退款/取消的前置判断，但不执行真实退款。

工具：

```text
refund_policy_check(orderId, tenantId)
```

产物：

- RefundPolicyCheckTool
- 风险判断结果

验收：

- 工具只能返回政策判断和建议动作。
- 不做真实资金操作。

### Day 15：Tool Calling 集成

目标：

- Agent 能根据意图选择工具。
- Web 调试台能展示工具调用明细。

产物：

- Spring AI Tool Calling 配置
- 工具调用集成测试
- Tool Calls 面板

验收：

- 用户问订单时调用 `order_lookup`。
- 用户要求退款时调用 `refund_policy_check`，并返回审批建议。
- Web 调试台能展示工具名、参数、耗时、状态和结果摘要。

## 阶段 4：RAG 知识库与多租户

### Day 16：整理知识库结构

目标：

- 建立 FAQ / 政策 / 产品知识目录。

产物：

```text
knowledge-base/
  default/
    faq/
    policies/
    products/
```

验收：

- 每份知识都有 source、tenant、version、category。

### Day 17：接入 Spring AI RAG

目标：

- 支持知识检索。

产物：

- Document Loader
- Text Splitter
- VectorStore
- retrieve_knowledge

验收：

- 用户问 FAQ 能返回答案和来源。

### Day 18：引入 pgvector

目标：

- 将 Day 17 的 `LocalKnowledgeEmbeddingModel` 替换为真实 `EmbeddingModel`，避免继续使用本地 hash / n-gram 占位向量判断语义质量。
- 从本地简单检索升级为可生产化向量库。

产物：

- 真实 EmbeddingModel 配置与可切换 fallback 策略
- PostgreSQL / pgvector 配置
- Docker Compose
- pgvector 和索引初始化 SQL 脚本

验收：

- 重新索引时使用真实 EmbeddingModel 生成向量，`LocalKnowledgeEmbeddingModel` 只保留为测试或离线 fallback。
- 远程 PostgreSQL 通过人工执行 SQL 脚本后，可完成向量写入和检索。

### Day 19：多租户隔离

目标：

- 实现租户级知识库和订单隔离。

产物：

- TenantContext
- TenantResolver
- `X-Tenant-ID`

验收：

- `tenant-a` 不能读到 `tenant-b` 数据。

### Day 20：知识库管理 API

目标：

- 支持知识库增删和重建索引。
- 将 Day 18 的启动全量重建索引改为显式 API / 增量索引流程，应用启动只做连接和表结构校验。
- Web 调试台能展示 RAG 来源和知识库调试结果。

接口：

```text
POST /api/v1/knowledge/items
DELETE /api/v1/knowledge/items
POST /api/v1/knowledge/reindex
```

验收：

- 新增知识无需重启即可检索。
- 删除知识后不再命中。
- 重启应用不会自动删除并重建全部向量数据。
- 知识内容未变化时，不重复调用 Embedding 模型生成相同向量。

## 阶段 5：MCP、Memory、安全与审批

### Day 21：设计 MCP 边界

目标：

- 明确哪些能力通过 MCP 暴露。

产物：

- MCP tools/resources/prompts 设计文档

验收：

- Agent 编排和工具实现解耦。

### Day 22：实现 MCP Server

目标：

- 暴露客服订单工具。

MCP Tools：

- kb_search
- order_lookup
- course_catalog
- refund_policy_check

验收：

- MCP Inspector 或测试客户端能列出工具并调用。

### Day 23：实现 MCP Client

目标：

- Agent App 通过 MCP 调用工具。

产物：

- MCP client 配置
- tool list 缓存
- tool call adapter

验收：

- Agent App 不直接依赖工具实现类。

### Day 24：Memory 与上下文压缩

目标：

- 支持多轮客服会话。

产物：

- ChatMemory
- 会话摘要
- 上下文裁剪策略

验收：

- 用户后续说“刚才那个订单”时能关联上下文。
- 上下文长度可控。

### Day 25：安全脱敏与审批

目标：

- 建立企业安全边界。

产物：

- RedactionFilter
- Prompt Injection 检查
- ApprovalRequest
- ToolPermissionGuard
- Approval Debug 页面

验收：

- 密码、身份证、银行卡、token 不进入日志明文。
- 退款/取消/改签不会绕过审批执行。
- Web 调试台能模拟审批请求，并展示脱敏后的 trace。

## 阶段 6：Agent 编排、观测、评测与部署

### Day 26：Agent Loop 编排

目标：

- 实现可追踪的执行链路。

流程：

```text
intent -> retrieve/tool -> risk check -> response -> trace
```

验收：

- 每次对话都有 traceId。
- trace 记录 route、工具调用、证据和最终回复。

### Day 27：多 Agent 扩展

目标：

- 只在必要处引入多 Agent。

候选角色：

- Intake Agent
- Knowledge Agent
- Order Agent
- Risk Agent
- Response Agent

验收：

- 能说明哪些角色现在需要，哪些暂不实现。
- 至少实现一个清晰分工的子 Agent 或保留设计文档。

### Day 28：Observability

目标：

- 接入企业可观测能力。
- 建立 Grafana Dashboard，而不是在 Web 调试台内自研完整监控台。

产物：

- Actuator
- Micrometer
- OpenTelemetry trace
- Prometheus metrics
- Grafana Dashboard 配置

验收：

- `/actuator/health` 可用。
- Grafana 能看到请求量、P95/P99 延迟、错误率、工具调用失败率、RAG 命中率。
- `customer-admin-web` 可以跳转 Grafana 或展示少量摘要，但不承担完整运行态监控职责。

### Day 29：Evals 与回归测试

目标：

- 建立 Agent 质量门禁。

eval case：

- FAQ 命中
- 订单查询
- 退款越权
- 敏感信息脱敏
- 跨租户隔离

验收：

- eval 能一键执行。
- 失败样本可定位到 prompt、工具或知识库。

### Day 30：Docker Compose 与端到端验收

目标：

- 完成可运行交付。
- 以 `<DEV_SERVER_HOST>` 作为默认远程部署目标，完成部署脚本和验收清单。

产物：

- Docker Compose
- 远程部署说明
- README 运行说明
- Grafana Dashboard 导入说明
- 端到端验收记录

验收：

- 一条命令启动依赖。
- 部署文档明确 SSH、目录、端口、容器、数据卷和回滚方式。
- Grafana Dashboard 可导入并看到核心指标。
- 跑通：
  - 智能问答
  - 订单查询
  - 退款审批建议
  - RAG 来源引用
  - trace / metrics

## 完成标准

30 天完成后，项目应具备：

- 可运行的 Spring Boot Agent 服务
- 可测试的 Tool Calling
- 可调用的 MCP Server / Client
- 可检索的知识库
- 可隔离的多租户数据
- 可审批的高风险操作
- 可追踪的执行链路
- 可运行的 eval cases
- 可部署的 Docker Compose

## 当前主线优先级

```text
P0：客服订单 MVP
P0：Spring AI Tool Calling
P0：RAG + 引用来源
P0：MCP tools/resources
P0：安全脱敏 + 审批

P1：多租户
P1：Memory / 上下文压缩
P1：Observability / Evals
P1：Docker Compose
P1：远程中间件连接与 SSH tunnel

P2：多 Agent
P2：完整运营管理后台
P2：移动端
P2：真实外部订单系统接入
```
