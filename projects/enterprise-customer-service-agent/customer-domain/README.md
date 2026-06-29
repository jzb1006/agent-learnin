# customer-domain

领域模型模块，负责承载客服订单平台的业务核心对象。

Day 03 已补齐核心领域模型：

- `Tenant`
- `CustomerOrder`
- `KnowledgeItem`
- `ConversationTrace`
- `ApprovalRequest`
- `ToolRiskLevel`

Day 11 已补齐工具契约模型：

- `ToolDefinition`
- `ToolParameterSchema`
- `ToolParameterType`
- `ToolPermission`
- `ToolResult`
- `ToolResultStatus`

## 边界

当前模块只表达领域语义，不绑定 Spring Web、Spring AI、MCP、JPA 或 JDBC。

| 包 | 职责 |
| --- | --- |
| `tenant` | 租户身份、启停状态和租户隔离基础 |
| `order` | 客户订单、订单状态和租户可见性 |
| `knowledge` | FAQ / 政策 / 产品知识条目的租户归属和启停 |
| `trace` | 对话路由、工具调用记录和不可变 trace |
| `approval` | 高风险动作审批请求 |
| `tool` | 工具定义、参数 schema、风险级别、权限策略和执行结果 |
| `support` | 领域内通用校验 |

## 验证

```bash
mvn -pl customer-domain test
```

当前覆盖：

- 租户 ID 必填和启用状态。
- 订单跨租户不可见。
- 知识条目停用后不可检索。
- trace 追加工具调用时保持原对象不变。
- 高风险动作必须进入审批。
- 只读工具不能创建审批请求。
- 工具定义必须包含名称、描述、参数 schema、风险等级和权限。
- 工具失败结果必须带明确错误码和错误信息。
