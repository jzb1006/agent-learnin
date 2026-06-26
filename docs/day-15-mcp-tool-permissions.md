# Day 15：实现 MCP 工具权限分类

## 今日目标

今日节点：Day 15

今日主题：MCP 工具权限分类。

今天解决的问题：让 MCP Tool 在工具发现阶段暴露业务权限元数据，并让 MCP Server 在执行前默认拒绝高风险工具。

今天不解决的问题：

- 不实现真实写操作工具。
- 不做人工审批流程。
- 不做动态用户权限、租户权限或远程鉴权。
- 不实现 MCP Resources、Prompts、Roots。

最终产物：

- MCP Tool `meta` 包含 `riskLevel` 和 `readOnly`。
- 低风险只读工具默认允许执行。
- 高风险或非只读工具在 handler 执行前被拒绝。
- `mcp-server` 测试覆盖工具元数据和高风险门禁。

验收标准：

- `ping`、`search_code`、`git_history`、`read_config` 的工具发现结果都包含 `riskLevel=LOW` 和 `readOnly=true`。
- 构造一个高风险写操作工具时，MCP Server 返回 `PERMISSION_DENIED`，且原始 handler 不会执行。
- `mvn -f projects/mcp-troubleshooting-agent/mcp-server/pom.xml test` 通过。

## 概念讲解

MCP 工具权限分类，是把工具能否默认执行这件事从“自然语言描述”变成机器可检查的元数据和执行门禁。

它解决三个问题：

- Agent 在 `tools/list` 阶段能看到工具风险，而不是只读工具描述文本。
- Server 在 `tools/call` 阶段有统一门禁，不依赖模型自觉。
- 后续引入写操作、审批和审计时，有稳定扩展点。

它不解决：

- 用户身份认证。
- 组织权限系统。
- 人工审批 UI。
- Prompt Injection 的完整防护。

和 Java 后端类比：`riskLevel/readOnly` 像接口元数据，`GuardedMcpToolSpecification` 像拦截器。Controller 或 Service 仍然实现业务逻辑，但请求进入业务逻辑前先经过权限判断。

## 项目映射

Day 14 结束时，MCP Server 已有 4 个工具：

```text
ping
search_code
git_history
read_config
```

Day 15 给这些工具增加统一包装：

```text
MCP Tool handler
  -> GuardedMcpToolSpecification
      -> 读取 McpToolMetadata
      -> defaultAllowed()
      -> 允许：执行原始 handler
      -> 拒绝：返回 PERMISSION_DENIED
```

当前元数据模型：

```text
McpToolMetadata
  riskLevel: LOW | MEDIUM | HIGH
  readOnly: boolean
```

当前默认策略：

```text
readOnly == true 且 riskLevel == LOW
  -> 默认执行

其他情况
  -> 默认拒绝
```

这符合第一阶段边界：只读排障 Agent 默认只允许查代码、查配置、查 Git 历史这类低风险只读工具。

## 实操任务

### 1. 增加权限元数据模型

新增：

- `McpToolRiskLevel`
- `McpToolMetadata`

`McpToolMetadata.toMeta()` 会输出可序列化结构：

```json
{
  "riskLevel": "LOW",
  "readOnly": true
}
```

该结构写入 MCP Java SDK 的 `McpSchema.Tool.meta` 字段。

### 2. 增加统一执行门禁

新增：

- `GuardedMcpToolSpecification`

它做两件事：

- 在工具定义上合并 `meta`。
- 在调用 handler 前检查 `metadata.defaultAllowed()`。

非默认允许工具返回：

```json
{
  "status": "PERMISSION_DENIED",
  "summary": "高风险工具或非只读工具默认不执行：restart_service",
  "evidence": [],
  "errorCode": "PERMISSION_DENIED",
  "errorMessage": "..."
}
```

### 3. 包装已有工具

`MinimalMcpServerApplication` 通过 `readOnlyLowRisk(...)` 包装：

- `ping`
- `search_code`
- `git_history`
- `read_config`

这样每个已注册工具共享同一份元数据策略，不需要在每个工具里重复写权限逻辑。

## 运行验证

先写失败测试：

```text
mvn -f projects/mcp-troubleshooting-agent/mcp-server/pom.xml \
  -Dtest=MinimalMcpServerApplicationTest#shouldExposeRiskMetadataForRegisteredTools+shouldRejectHighRiskToolBeforeExecutingHandler \
  test
```

红灯证据：

```text
找不到符号：GuardedMcpToolSpecification
找不到符号：McpToolMetadata
```

实现后重跑新增测试：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

最终跑 `mcp-server` 全量测试：

```text
mvn -f projects/mcp-troubleshooting-agent/mcp-server/pom.xml test
```

验证结果：

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 代码审查要点

- KISS：只做 `riskLevel/readOnly` 和默认门禁，不提前做审批系统。
- YAGNI：不新增真实写工具，只用测试构造高风险工具验证门禁。
- DRY：只读低风险包装集中在 `readOnlyLowRisk(...)`。
- SOLID：工具业务逻辑不关心权限元数据，门禁由 `GuardedMcpToolSpecification` 负责。

## 复盘提问

1. 为什么不能只靠工具描述里写“这是高风险工具，请不要调用”？
2. `readOnlyHint` 和本项目自己的 `readOnly` 元数据有什么区别？
3. 为什么高风险工具要在 handler 执行前拒绝，而不是执行后再标记失败？

## 今日结论

Day 15 的核心不是实现写操作，而是建立“工具发现可见 + Server 执行前强制”的权限分类闭环。MCP annotations 可以给模型提示工具性质，但生产边界必须由 Server 侧元数据和门禁共同保证。
