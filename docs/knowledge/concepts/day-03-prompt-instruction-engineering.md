# Day 03：Prompt / Instruction Engineering

## 一句话定义

Prompt / Instruction Engineering 是把模型的角色、任务、边界、工具规则和输出格式写成清晰、可测试的指令。

## 解决的问题

它解决的是模型行为不可控的问题。

在排障场景中，如果没有清晰指令，模型容易：

- 凭经验猜原因。
- 把推断说成事实。
- 忽略证据来源。
- 给出越权操作建议。
- 输出格式不稳定，后续无法解析和评测。

## 不解决的问题

Prompt 不负责解决所有业务问题。

它不应该承担：

- 保存具体接口文档。
- 保存历史故障案例。
- 替代代码搜索、配置读取和 Git 查询。
- 替代 RAG 知识库。
- 替代权限控制和工具参数校验。

## 在本项目中的位置

Prompt 位于 `agent-app` 的编排入口附近，负责约束 Agent 行为。

后续模块分工：

- `agent-app`：加载 system/developer instruction，组织诊断报告。
- `mcp-server`：提供只读工具和 tool instruction。
- `knowledge-base`：提供业务知识、排障手册和历史案例。
- `evals`：验证 Prompt 是否守住格式、证据和安全边界。

## 最小代码证据

Day 03 不写 Java 代码。

今天的最小证据是三类指令草案：

- system instruction：定义只读排障 Agent 身份和安全边界。
- developer instruction：定义 MVP 工作流程和报告约束。
- tool instruction：定义后续只读工具的用途、权限、返回值和失败语义。

## 常见误区

- 误区一：Prompt 越长越安全。
- 误区二：把业务逻辑写进 Prompt 更快。
- 误区三：模型会自动遵守输出格式，不需要约束。
- 误区四：只要 Prompt 写了禁止操作，就不需要工具层权限控制。
- 误区五：工具失败时可以让模型猜结果。

## 自测问题

1. system instruction、developer instruction、tool instruction 分别适合放什么？
2. Prompt 为什么不能替代 RAG 知识库？
3. 为什么诊断报告要强制写证据来源？
4. 用户要求越权操作时，Prompt 应该如何约束 Agent？
5. 工具结果被裁剪时，报告应该怎么表达？

## 今日结论

Day 03 的核心是给 Agent 建立行为契约。Prompt 负责角色、边界、流程和格式，不负责承载业务事实；业务事实应该来自工具和知识库。
