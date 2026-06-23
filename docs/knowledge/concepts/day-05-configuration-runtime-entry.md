# Day 05：配置管理与运行入口

## 一句话定义

配置管理是把运行时可变、环境相关、敏感或用户可选择的信息从代码中剥离出来，并用明确边界控制 Agent 能访问什么、如何运行、失败时如何定位。

## 解决的问题

它解决的是 Agent 工程里的运行边界问题：

- 模型 API Key 不能写死或误提交。
- 目标项目路径不能写死在代码中。
- 只读工具不能越过允许根目录。
- CLI、REST 和测试入口不能各自定义一套行为。
- 出错时要能区分配置缺失、路径越界、模型失败和工具失败。

本项目后续测试模型默认使用 DeepSeek。配置命名采用 `DEEPSEEK_API_KEY`、`DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL`，但 Java 代码层仍应保留 OpenAI-compatible 模型客户端抽象，避免业务逻辑绑定具体厂商。

## 不解决的问题

Day 05 不解决：

- 真实 LLM API 调用。
- Spring Boot 工程创建。
- MCP Client / Server 实现。
- CLI 或 REST Controller 实现。
- RAG、Memory、Agent Loop。

今天只定义配置和入口契约。

## 在本项目中的位置

```text
projects/mcp-troubleshooting-agent/
  agent-app/
    配置加载
    模型参数
    Agent运行参数
    CLI/REST入口草案
  mcp-server/
    只读根目录校验
    工具路径约束
    敏感值脱敏
  knowledge-base/
    知识库路径
  traces/
    trace输出路径
```

## 最小代码证据

Day 05 不写 Java 代码。

今天的最小证据是：

- `docs/day-05-configuration-runtime-entry.md`
- `docs/knowledge/concepts/day-05-configuration-runtime-entry.md`
- `docs/knowledge/mindmaps/day-05-configuration-runtime-entry.md`
- `docs/knowledge/decisions/day-05-configuration-entry-boundary.md`

## 常见误区

- 误区一：先把 API Key 写到配置文件里，后面再改。
- 误区二：目标项目路径写死成本地机器路径。
- 误区三：Agent App 自己读文件，绕过 MCP Server 的路径校验。
- 误区四：CLI 和 REST 分别实现两套参数逻辑。
- 误区五：Day 05 顺手把模型调用和 Controller 都写出来。
- 误区六：因为测试 DeepSeek，就把所有模型调用代码命名成 DeepSeek 专用类。

## 自测问题

1. 什么配置可以提交到仓库，什么配置不能提交？
2. 为什么目标项目路径必须可配置？
3. 只读根目录校验应该放在 Agent App 还是工具层？为什么？
4. CLI 和 REST 入口的职责边界是什么？
5. 为什么今天不实现真实 LLM API 调用？

## 今日结论

Day 05 的核心是先把运行边界定义清楚。配置不是附属细节，而是排障 Agent 能否安全、可复现、可测试运行的基础。
