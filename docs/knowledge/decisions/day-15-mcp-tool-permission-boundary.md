# ADR：Day 15 MCP 工具权限分类边界

## 背景

Day 14 已把 `search_code`、`git_history` 和 `read_config` 都迁移到 MCP Server。它们都有 MCP `ToolAnnotations`，例如 `readOnlyHint=true` 和 `destructiveHint=false`。

这些 hint 对模型选择工具有帮助，但还不是本项目的业务权限模型，也不能阻止 Server 执行高风险工具。Day 15 需要建立最小权限分类闭环。

## 决策

采用 MCP Tool `meta` 承载本项目权限元数据：

```json
{
  "riskLevel": "LOW",
  "readOnly": true
}
```

并新增 `GuardedMcpToolSpecification`：

- 包装原始 `SyncToolSpecification`。
- 合并 `McpToolMetadata` 到 `McpSchema.Tool.meta`。
- 在 handler 执行前检查默认策略。
- 默认只允许 `readOnly=true && riskLevel=LOW`。
- 其他工具返回 `PERMISSION_DENIED`，不调用原始 handler。

## 备选方案

### 方案 A：只使用 MCP ToolAnnotations

优点是不用新增项目模型。

缺点是 `readOnlyHint` 是工具语义提示，不是完整业务权限分类；没有 `riskLevel`，也不能表达本项目的默认执行策略。

### 方案 B：把 `riskLevel/readOnly` 写进工具 description

优点是模型可读。

缺点是不可稳定解析，测试只能断言字符串，Server 侧门禁也不能依赖自然语言。

### 方案 C：使用 MCP Tool meta + Server 侧 wrapper

优点是机器可读、Agent 可见、Server 可强制、测试可断言。

缺点是需要维护一层项目级权限模型。

## 取舍

选择方案 C。

原因：

- KISS：只新增两个元数据字段和一个 wrapper。
- YAGNI：不提前实现真实写操作、人审和动态权限系统。
- DRY：权限门禁集中在 `GuardedMcpToolSpecification`。
- SOLID：具体工具只负责业务执行，权限策略独立演进。

## 后果

正向后果：

- `tools/list` 可验证每个工具的业务权限元数据。
- 高风险工具即使被模型请求，也不会进入原始 handler。
- Day 20 contract test 可以直接验证 `meta` 和拒绝语义。
- 后续 Human-in-the-loop 可以基于同一元数据扩展审批策略。

代价：

- 本项目维护一套 MCP annotations 之外的权限元数据。
- 当前默认策略只有低风险只读允许执行，写操作只能作为测试对象存在。

## 复查条件

出现以下情况时复查该决策：

- 引入真实写操作工具。
- 需要按用户、环境或目标项目动态授权。
- MCP SDK 对 tool annotations 增加标准化风险字段。
- 需要把审批结果写入 trace 或 audit log。
