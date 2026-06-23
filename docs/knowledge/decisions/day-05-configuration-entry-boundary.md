# Day 05 决策记录：先定义配置和入口契约，不提前实现运行入口

## 背景

Day 01-04 已经确定：

- 本项目是 MCP 优先的 Java 后端只读排障 Agent。
- 第一阶段只做只读诊断，不执行写操作。
- 项目结构已经拆成 `agent-app`、`mcp-server`、`knowledge-base`、`evals`、`traces`。

Day 05 需要设计模型配置、目标项目路径、只读根目录、环境变量和 CLI / REST 入口。这里的关键取舍是：今天是只定义契约，还是直接实现可运行入口。

后续模型测试默认使用 DeepSeek。DeepSeek 提供 OpenAI-compatible API，因此配置层采用 DeepSeek 环境变量命名，代码层后续仍保留兼容模型客户端抽象。

## 决策

先定义配置结构和 CLI / REST 入口草案，不创建真实 Spring Boot 工程，也不实现命令行或 HTTP 接口。

当前阶段只固化：

- 配置分层。
- 环境变量命名。
- 敏感值处理规则。
- 目标项目和只读根目录边界。
- CLI `diagnose` 命令草案。
- REST `POST /api/diagnostics` 草案。

模型相关环境变量采用：

- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`

## 备选方案

### 方案 A：只定义配置和入口契约

优点：

- 符合 Day 05 的学习目标。
- 不提前进入 Day 06 的模型调用和 Day 41 的入口实现。
- 能先讲清安全边界和运行参数。
- 后续实现时有稳定契约可对照。

缺点：

- 今天仍没有可运行应用。

### 方案 B：直接创建 Spring Boot 工程并实现 REST Controller

优点：

- 看起来更接近真实应用。
- 可以更早启动服务。

缺点：

- 会把学习重点从配置边界转移到框架样板。
- 容易提前引入鉴权、错误处理、序列化和部署问题。
- Day 06 的 LLM API 学习目标会被混入 Controller 实现。

### 方案 C：只定义 CLI，不定义 REST

优点：

- 更简单，符合本地学习场景。

缺点：

- 后续服务化入口缺少早期契约。
- Day 41 做 REST 时可能重新发明请求和响应结构。

## 结论

采用方案 A，并保留 REST 草案。

第一阶段优先把 CLI 作为最小运行入口方向；REST 作为后期服务化入口的契约预留，但不提前实现。

## 影响

- Day 06 可以基于 `model.*` 配置实现最小 LLM API 调用。
- Day 09-10 可以基于 `target-project.readonly-roots` 实现只读工具路径校验。
- Day 12 以后可以基于 `mcp.server.*` 配置启动或连接 MCP Server。
- Day 38 以后可以基于 `trace.*` 配置落地观测记录。
- Day 41 可以按今天的 CLI / REST 草案实现真实运行入口。

## 验证方式

- 配置结构能覆盖模型、目标项目、MCP、知识库、trace 和安全边界。
- 敏感配置没有真实值进入仓库。
- 目标项目访问路径可配置。
- 只读根目录边界明确。
- 没有提前实现 LLM API、CLI、REST、MCP 或 Tool Calling。
