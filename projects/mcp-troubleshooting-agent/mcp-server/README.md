# mcp-server

`mcp-server` 负责把排障能力标准化暴露给 Agent。

## 负责

- 暴露只读 MCP Tools，例如 `search_code`、`read_config`、`git_history`。
- 暴露 MCP Resources，例如项目元信息、接口文档摘要、配置摘要。
- 暴露 MCP Prompts，例如常见排障流程模板。
- 管理 MCP Roots 和允许访问的目标项目目录。
- 对工具输入做校验，对敏感配置做脱敏。
- 返回结构化、可追溯、可裁剪的工具结果。

## 不负责

- 不决定最终诊断结论。
- 不把自然语言业务推理写进工具实现。
- 不执行默认写操作。
- 不通过路径穿越读取允许根目录外的文件。

## MCP 建模规则

- 主动查询或计算：建模为 Tool。
- 稳定只读上下文：建模为 Resource。
- 可复用交互流程：建模为 Prompt。
- 访问目录边界：建模为 Roots 和服务端校验。

## 当前代码

Day 12 已新增最小 MCP Server：

```text
src/main/java/io/github/jiangzhibin/agentlearning/mcpserver/
  MinimalMcpServerApplication.java
```

当前只暴露一个 `ping` 工具：

- 无参数。
- 返回文本 `pong`。
- 标记为只读、幂等、非破坏性。
- 通过 stdio transport 提供 MCP 协议能力。

Day 13 之后再把 `search_code`、`git_history`、`read_config` 逐步迁移进来。

## 本地验证

打包可执行 MCP Server JAR：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" package
```

运行 `mcp-server` 单元测试：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/mcp-server/pom.xml" test
```

Agent 侧 stdio 调用验证：

```bash
mvn -f "/Users/jiangzhibin/workspace/agent-learning/projects/mcp-troubleshooting-agent/agent-app/pom.xml" test -Dtest=MinimalMcpClientTest
```

注意：stdio MCP Server 的 stdout 只能写 MCP 协议消息，普通日志必须写 stderr 或使用 SDK 日志能力。
