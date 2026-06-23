# Day 01：Agent 与普通 LLM 应用

## 一句话定义

AI Agent 是围绕目标持续计划、调用工具、观察结果并调整下一步的受控执行系统；普通 LLM 应用通常是一次输入对应一次输出。

## 解决的问题

Agent 解决的是“单次回答无法完成”的任务，例如排障、调研、代码审查和多源证据分析。

这些任务往往需要：

- 先拆解目标。
- 再选择工具。
- 根据工具结果调整假设。
- 最后输出可追溯结论。

## 不解决的问题

Agent 不自动保证答案正确。

如果没有清晰边界、可靠工具、结构化输出、trace 和评测，Agent 只会把一次不可靠回答扩展成多次不可靠行动。

## 在本项目中的位置

本项目的 Agent 位于排障流程编排层。

它不直接承担所有能力，而是协调：

- LLM：理解问题、生成计划、总结报告。
- MCP Tools：执行只读证据查询。
- RAG：检索文档、手册和历史案例。
- Memory：保留会话过程和长期项目事实。
- Trace / Evals：记录调用链并验证是否退化。

## 最小代码证据

Day 01 先不写业务代码，只确定工作流边界。

后续代码会逐步落到：

- `projects/mcp-troubleshooting-agent/agent-app`
- `projects/mcp-troubleshooting-agent/mcp-server`
- `projects/mcp-troubleshooting-agent/knowledge-base`
- `projects/mcp-troubleshooting-agent/evals`
- `projects/mcp-troubleshooting-agent/traces`

## 常见误区

- 误区一：只要用了 LLM 就是 Agent。
- 误区二：只要模型能调用工具就是 Agent。
- 误区三：Agent 可以替代权限控制和人工审批。
- 误区四：排障报告看起来合理就算完成。
- 误区五：先把所有高级能力加上，后面再收敛边界。

## 自测问题

1. 普通 LLM 应用和 Tool Calling 应用的关键区别是什么？
2. Tool Calling 应用和 Agent 应用的关键区别是什么？
3. 为什么排障 Agent 必须记录证据来源？
4. 为什么第一阶段只做只读工具？

## 今日结论

本项目要构建的不是聊天机器人，而是一个受控的只读排障 Agent。它的第一目标是可靠地收集证据、收敛诊断路径，并把事实和推断分开。
