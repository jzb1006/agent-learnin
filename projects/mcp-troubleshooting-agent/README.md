# MCP Troubleshooting Agent

这是一个面向 Java 后端项目的 MCP 优先只读排障 Agent。

第一阶段目标不是自动修复问题，而是基于本地只读证据生成可追溯的诊断报告。

## 模块边界

```text
mcp-troubleshooting-agent/
  agent-app/        # Agent 编排、模型调用、报告生成
  mcp-server/       # MCP 工具、资源、提示词模板和访问边界
  knowledge-base/   # 本地接口文档、排障手册和历史案例
  evals/            # 报告格式、工具边界和安全规则评测样例
  traces/           # 工具调用链路、观察结果和诊断过程记录
```

## 当前阶段约束

- 只读优先：不重启服务、不部署、不改配置、不写数据库、不调用生产 API。
- Agent 与工具解耦：Agent 负责编排和判断，MCP Server 负责暴露能力。
- 事实来源外置：业务事实来自工具结果、MCP Resources、知识库或用户输入，不写死在 Prompt 中。
- 证据可追溯：诊断报告必须标注证据来源，证据不足时明确写出不确定点。

## 后续演进

- Day 05：补配置结构和运行入口草案。
- Day 06-10：实现 LLM API、结构化输出和本地只读工具。
- Day 11-12：理解 MCP 角色，并搭建暴露 `ping` 的最小 stdio MCP Server。
- Day 13-20：把只读工具迁移为 MCP Tools，并补齐 Resources、Prompts、Roots 和协议测试。
- Day 21 以后：接入 RAG、Memory、上下文压缩、Agent 编排、观测和评测。
