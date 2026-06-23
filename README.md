# Agent 应用工程学习路径

目标：从 Java 后端开发转向 AI Agent 应用工程，重点掌握 LLM API、工具调用、RAG、Agent 编排、MCP、评测与生产化。

## 学习入口

- [45 天每日学习路线](docs/AI_AGENT_DAILY_LEARNING_PATH.md)
- [AI Agent 教学路线与带学方案](docs/AI_AGENT_TEACHING_GUIDE.md)

## 学习阶段

### 1. LLM API 基础

目录：`01-llm-api`

目标：
- 理解 token、上下文窗口、temperature、structured output。
- 能用 Java/Spring Boot 调用主流 LLM API。
- 掌握超时、重试、错误处理和响应解析。

推荐资料：
- OpenAI API 文档：https://developers.openai.com/api
- Anthropic Prompt Engineering：https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/overview

### 2. Tool Calling

目录：`02-tool-calling`

目标：
- 理解工具 schema、参数校验、幂等性和权限控制。
- 能让模型调用 Java 后端方法。
- 能区分普通问答、结构化输出和工具调用。

推荐资料：
- OpenAI Function Calling：https://developers.openai.com/api/docs/guides/function-calling
- OpenAI Structured Outputs：https://developers.openai.com/api/docs/guides/structured-outputs

### 3. RAG

目录：`03-rag`

目标：
- 掌握文档切分、embedding、向量检索、重排、引用来源。
- 能构建一个 Java 文档问答系统。
- 能评估召回质量，而不是只看回答是否像真的。

推荐资料：
- OpenAI Cookbook：https://developers.openai.com/cookbook
- pgvector：https://github.com/pgvector/pgvector
- Qdrant：https://qdrant.tech/documentation/

### 4. Agent 编排

目录：`04-agent-orchestration`

目标：
- 理解 Agent loop：计划、行动、观察、继续执行。
- 掌握状态机式 Agent 编排。
- 能处理失败重试、人工确认和执行边界。

推荐资料：
- Hugging Face Agents Course：https://huggingface.co/learn/agents-course/en/unit0/introduction
- DeepLearning.AI Agentic AI：https://www.deeplearning.ai/courses/agentic-ai
- LangGraph Academy：https://academy.langchain.com/courses/intro-to-langgraph

### 5. MCP

目录：`05-mcp`

目标：
- 理解 MCP 是 Agent 连接外部系统的标准协议。
- 能用 Java 封装业务工具服务。
- 能设计安全的工具边界、权限和审计。

推荐资料：
- MCP 官方文档：https://modelcontextprotocol.io/docs/getting-started/intro
- MCP Specification：https://modelcontextprotocol.io/specification/2025-06-18

### 6. 评测、观测与生产化

目录：`06-evals-observability`

目标：
- 建立 Agent 评测集。
- 记录 trace、成本、失败样本和工具调用链路。
- 设计敏感操作审批机制。

推荐资料：
- OpenAI Evals Cookbook：https://developers.openai.com/cookbook/topic/evals
- LangSmith Academy：https://academy.langchain.com/collections/products

## Java 方向框架

优先级建议：

1. Spring AI：https://spring.io/projects/spring-ai
2. Spring AI Reference：https://docs.spring.io/spring-ai/reference/index.html
3. LangChain4j：https://docs.langchain4j.dev/
4. Google ADK Java：https://adk.dev/get-started/java/

## 实战项目

目录：`projects`

建议项目：Java 后端排障 Agent

能力范围：
- 查日志。
- 查接口文档。
- 查只读数据库数据。
- 查 Git 提交记录。
- 总结可能原因。
- 给出修复建议。
- 对高风险操作加入人工确认。

原则：
- KISS：先做最小可用闭环。
- YAGNI：不提前设计复杂多 Agent 系统。
- DRY：工具调用、模型调用、日志记录要统一封装。
- SOLID：工具能力与 Agent 编排解耦，避免业务逻辑写死在 Prompt 中。
