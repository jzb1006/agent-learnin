# Day 02：创建多模块项目骨架

## 目标

为“企业级智能客服与订单协同 Agent 平台”创建可维护的工程骨架。

Day 02 不追求业务功能，而是确保后续 Day 03-30 都有清晰落点：

- Java 多模块工程可以被 IDE 和 Maven 正确识别。
- Web 调试台可以在本地启动。
- 后端和前端都有最小测试链路。
- 版本基线在创建工程当天锁定，避免文档和代码脱节。

## 当前产物

```text
projects/enterprise-customer-service-agent/
  pom.xml
  customer-agent-app/
  customer-domain/
  customer-mcp-server/
  customer-admin-web/
  knowledge-base/
  evals/
  traces/
  deploy/
```

## 模块边界

| 模块 | 当前职责 | 后续接入点 |
| --- | --- | --- |
| `customer-domain` | 领域模型边界 | Day 03 领域模型与单元测试 |
| `customer-agent-app` | Spring Boot 应用入口 | Day 04 REST API，Day 06 LLM，Day 26 Agent Loop |
| `customer-mcp-server` | MCP Server 边界 | Day 21-23 MCP tools/resources/prompts |
| `customer-admin-web` | 本地调试台基础页面 | Day 04 基础 API，Day 10 结构化响应，Day 15 Tool Calls |

## 版本选择

| 技术 | 锁定版本 | 说明 |
| --- | --- | --- |
| Java | 21 | 本机验证 JDK，企业项目稳妥起步 |
| Spring Boot | 4.1.0 | Maven Central 当前 release |
| Spring AI | 2.0.0 | 通过 BOM 预留后续 AI / MCP 依赖管理 |
| Node.js | 24.18.0 LTS | 已用 nvm 设为默认版本 |
| Vite | 8.1.0 | 当前可用版本，支持 Node 24 |
| React | 19.2.7 | 当前可用版本 |
| TypeScript | 6.0.3 | 当前可用版本 |
| Ant Design | 6.4.5 | 当前可用版本 |
| TanStack Query | 5.101.1 | 当前可用版本 |

## 验证方式

后端：

```bash
cd projects/enterprise-customer-service-agent
mvn test
```

前端：

```bash
cd projects/enterprise-customer-service-agent/customer-admin-web
npm test
npm run build
npm run dev
```

## 学习重点

### 为什么先分模块

Agent 项目后期会同时包含对话入口、领域模型、工具、MCP、RAG、审批、安全和观测。如果一开始都放进单模块，后续会很难解释“业务规则在哪里”“工具边界在哪里”“MCP 只是协议暴露还是业务实现”。

Day 02 的分层先把职责隔开：

- `customer-domain` 保存业务语言。
- `customer-agent-app` 面向用户请求和 Agent 编排。
- `customer-mcp-server` 面向外部工具协议。
- `customer-admin-web` 面向开发调试。

### 为什么暂不引入 Spring AI starter

Spring AI 是主线，但 Day 02 还没有模型调用、Tool Calling、RAG 或 MCP 行为。现在只导入 Spring AI BOM，先统一后续依赖版本，不提前引入未使用 starter。

这符合 YAGNI：需要 ChatClient 时在 Day 06 引入；需要 MCP 时在 Day 21 引入。

### 为什么 Web 调试台是 P0

Agent 应用不能只看接口是否返回 200。开发者需要看到：

- route
- riskLevel
- nextActions
- traceId
- tool calls
- RAG sources

所以 `customer-admin-web` 从 Day 02 就进入工程，但当前只保留基础页面，避免过早做完整后台。

## 原则应用

- KISS：只实现模块锚点、启动入口和基础页面。
- YAGNI：不接数据库、不接 Redis、不接模型、不写业务 API。
- DRY：Java 版本和依赖版本集中在父 `pom.xml`；前端版本集中在 `package.json`。
- SOLID：每个模块单一职责，后续 Day 的实现可以落到明确边界内。

## 下一步

Day 03 进入 `customer-domain`，补齐领域模型和单元测试：

- `Tenant`
- `CustomerOrder`
- `KnowledgeItem`
- `ConversationTrace`
- `ApprovalRequest`
- `ToolRiskLevel`
