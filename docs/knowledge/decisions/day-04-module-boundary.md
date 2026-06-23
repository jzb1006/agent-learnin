# Day 04 决策记录：先建立模块边界，不提前创建完整工程

## 背景

Day 01-03 已经确定：

- 本项目是 MCP 优先的 Java 后端只读排障 Agent。
- 第一阶段只做只读诊断，不做自动修复。
- Prompt 只定义行为契约，不承载业务事实。

Day 04 需要初始化项目结构。这里有一个关键取舍：是直接创建完整 Spring Boot 多模块工程，还是先创建最小目录骨架和职责说明。

## 决策

采用最小目录骨架：

```text
projects/mcp-troubleshooting-agent/
  agent-app/
  mcp-server/
  knowledge-base/
  evals/
  traces/
```

每个目录先保留 `README.md`，明确职责边界。

暂不创建：

- `pom.xml`
- Spring Boot 启动类
- MCP Server 实现类
- RAG 或 Memory 代码
- 多 Agent 编排目录

## 备选方案

### 方案 A：先建立最小目录骨架

优点：

- 符合 Day 04 的学习目标。
- 避免提前进入 Day 05/06 的配置和模型调用。
- 易于解释模块职责。
- 后续可根据实际课程逐步演进。

缺点：

- 今天没有可运行应用。

### 方案 B：直接创建完整 Spring Boot 多模块工程

优点：

- 看起来更接近真实工程。
- 后续可以直接写代码。

缺点：

- 容易提前引入构建、依赖、配置、启动方式等 Day 05/06 内容。
- 学习重点会从模块边界转移到框架细节。
- 可能创建暂时用不到的抽象，违反 YAGNI。

### 方案 C：只写文档，不创建项目目录

优点：

- 最轻量。

缺点：

- 缺少实际项目落点。
- 后续实现时仍要重新做结构决策。

## 结论

采用方案 A：先建立最小目录骨架，并用 README 固化职责边界。

## 影响

- Day 05 可以在这个结构上继续定义配置文件和运行入口草案。
- Day 06-10 可以逐步在 `agent-app` 中实现 LLM API、结构化输出和本地只读工具适配。
- Day 11-20 可以把只读工具迁移到 `mcp-server`，并补齐 MCP Resources、Prompts、Roots。
- Day 21 以后 `knowledge-base`、`evals`、`traces` 已经有稳定落点。

## 验证方式

- 目录结构存在。
- 每个模块有明确职责说明。
- 没有提前创建 Spring Boot、MCP、RAG 或 Memory 实现。
- 学习进度不在用户完成复习验收前标记为完成。
