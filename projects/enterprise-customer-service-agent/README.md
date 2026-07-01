# 企业级智能客服与订单协同 Agent 平台

## 定位

这是 30 天路线的主工程目录。当前已完成多模块骨架、核心领域模型、基础 REST API、本地调试台、Spring AI ChatClient、意图识别、结构化回复、Tool Calling、RAG、多租户、知识库管理、MCP Server / Client，以及 `/chat` 短期 Memory 与上下文压缩。

## 模块

| 模块 | 职责 | 当前状态 |
| --- | --- | --- |
| `customer-domain` | 租户、订单、知识库、审批、trace 等领域模型 | Day 03 已补核心领域模型和单元测试 |
| `customer-agent-app` | Spring Boot 对话入口、Agent 编排、REST API | 已通过 MCP Client 调用工具，并支持 Redis 短期 Memory |
| `customer-mcp-server` | MCP tools/resources/prompts 暴露 | 已暴露客服订单相关 tools/resources/prompts |
| `customer-admin-web` | 本地 Agent 调试台 | 已展示 chat、tools、RAG sources、订单和 Memory 调试信息 |
| `knowledge-base` | FAQ / 政策 / 产品知识样例 | 已填充默认租户知识库样例 |
| `evals` | Agent 回归评测用例 | Day 29 开始填充 |
| `traces` | 本地 trace 和审计样例 | Day 26 开始填充 |
| `deploy` | Docker Compose、SQL、观测配置 | Day 18 后逐步填充 |

## 版本基线

| 类型 | 当前锁定 |
| --- | --- |
| Java | 21 |
| Spring Boot BOM | 4.1.0 |
| Spring AI BOM | 2.0.0 |
| Lombok | 1.18.46 |
| Node.js | 24.18.0 LTS |
| npm | 11.16.0 |
| Vite | 8.1.0 |
| React | 19.2.7 |
| TypeScript | 6.0.3 |
| Ant Design | 6.4.5 |
| TanStack Query | 5.101.1 |

## 本地验证

后端多模块测试：

```bash
mvn test
```

前端调试台测试：

```bash
cd customer-admin-web
npm test
```

前端调试台构建：

```bash
cd customer-admin-web
npm run build
```

前端本地启动：

```bash
cd customer-admin-web
npm run dev
```

## 设计原则

- KISS：每个 Day 只推进当前业务闭环需要的能力，Redis 仅用于短期 Memory，pgvector 仅用于知识库向量检索。
- YAGNI：暂不实现复杂 Agent Loop、多 Agent、真实退款/取消/改签和完整运营后台。
- DRY：版本集中在父 `pom.xml` 和前端 `package.json`，避免模块重复锁版本。
- SOLID：领域、应用入口、MCP 暴露和调试台分离，后续可以独立演进和测试。
