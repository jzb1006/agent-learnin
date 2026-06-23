# agent-app

`agent-app` 负责排障 Agent 的应用层编排。

## 负责

- 加载 system instruction 和 developer instruction。
- 接收用户排障问题。
- 决定需要哪些只读证据。
- 调用模型、MCP Client 或本地适配层。
- 汇总证据并生成诊断报告。
- 写入 trace 和评测所需的结构化观察结果。

## 不负责

- 不直接实现代码搜索、Git 查询或配置读取细节。
- 不绕过 MCP Server 访问目标项目。
- 不保存具体业务事实和历史故障知识。
- 不执行重启、部署、改配置、写数据库或生产 API 调用。

## 设计原则

- KISS：先保留一个诊断入口，不提前做多 Agent 编排。
- YAGNI：未接入真实 LLM 前，不创建复杂抽象层。
- DRY：报告结构、工具结果和 trace 字段后续统一定义。
- SOLID：Agent 编排依赖工具契约，不依赖具体工具实现。
