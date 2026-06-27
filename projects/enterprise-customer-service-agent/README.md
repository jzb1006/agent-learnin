# 企业级智能客服与订单协同 Agent 平台

## 定位

这是 30 天路线的主工程目录。Day 02 只建立可被 IDE 识别、可测试、可扩展的多模块骨架，不提前实现订单、RAG、MCP 或 LLM 业务逻辑。

## 模块

| 模块 | 职责 | 当前状态 |
| --- | --- | --- |
| `customer-domain` | 租户、订单、知识库、审批、trace 等领域模型 | Day 03 已补核心领域模型和单元测试 |
| `customer-agent-app` | Spring Boot 对话入口、Agent 编排、REST API | Day 02 可启动骨架；Day 04 补基础 API |
| `customer-mcp-server` | MCP tools/resources/prompts 暴露 | Day 02 仅有模块锚点；Day 21 后实现 MCP |
| `customer-admin-web` | 本地 Agent 调试台 | Day 02 可启动基础页面；后续接入 Chat、Order、Tool、RAG、Approval |
| `knowledge-base` | FAQ / 政策 / 产品知识样例 | Day 16 开始填充 |
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

前端本地启动：

```bash
cd customer-admin-web
npm run dev
```

## 设计原则

- KISS：Day 02 只做工程边界和运行链路，不做业务提前实现。
- YAGNI：不提前引入 Spring AI starter、MCP starter、数据库或 Redis 连接。
- DRY：版本集中在父 `pom.xml` 和前端 `package.json`，避免模块重复锁版本。
- SOLID：领域、应用入口、MCP 暴露和调试台分离，后续可以独立演进和测试。
