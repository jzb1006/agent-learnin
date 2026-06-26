# Day 15：MCP 工具权限分类

## 一句话定义

MCP 工具权限分类是把工具风险、只读属性和默认执行策略变成机器可检查的元数据与 Server 侧门禁。

## 解决的问题

- Agent 在工具发现阶段能看到 `riskLevel` 和 `readOnly`。
- MCP Server 能在 handler 执行前统一拒绝高风险工具。
- 后续引入写操作、人审和审计时，有稳定权限扩展点。
- 工具权限不再只依赖自然语言描述或模型自觉。

## 不解决的问题

- 不实现真实写操作。
- 不实现用户身份认证或组织权限。
- 不实现审批 UI。
- 不完整覆盖 Prompt Injection。

## 在本项目中的位置

```text
mcp-server
  MinimalMcpServerApplication
    -> readOnlyLowRisk(...)
    -> GuardedMcpToolSpecification
      -> McpToolMetadata
      -> McpToolRiskLevel
      -> 原始工具 handler
```

## 最小代码证据

- `McpToolRiskLevel` 定义 `LOW / MEDIUM / HIGH`。
- `McpToolMetadata` 定义 `riskLevel / readOnly`，并输出 MCP Tool `meta`。
- `GuardedMcpToolSpecification` 合并工具 `meta`，并在执行前调用 `defaultAllowed()`。
- `MinimalMcpServerApplication` 使用 `readOnlyLowRisk(...)` 包装 `ping`、`search_code`、`git_history` 和 `read_config`。
- `MinimalMcpServerApplicationTest` 验证已注册工具元数据和高风险工具拒绝执行。

## 常见误区

- 以为 MCP `readOnlyHint` 已经等于权限控制。
- 只把风险写在工具描述里，缺少机器可检查字段。
- 让模型决定是否执行高风险工具，而不是让 Server 侧强制拦截。
- 为了验证权限门禁而过早实现真实重启、部署或写数据库工具。
- 在每个工具 handler 内重复写权限逻辑，导致策略不一致。

## 自测问题

1. 为什么不能只靠工具描述约束高风险工具？
2. `riskLevel` 和 `readOnly` 分别解决什么问题？
3. 为什么高风险工具必须在 handler 执行前被拒绝？

## 今日结论

Day 15 把 MCP 工具从“可发现、可调用”推进到“可分类、可治理”。第一阶段只读 Agent 的默认策略很简单：低风险只读工具允许执行，其他工具默认拒绝。
