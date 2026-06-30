# customer-mcp-server

客服订单 MCP Server 模块，负责把客服订单平台能力暴露为 MCP tools、resources 和 prompts。

Day 21 已固定 MCP 边界设计：

- 设计文档：[Day 21：设计 MCP 边界](../../../docs/day-21-mcp-boundary-design.md)
- 实现文档：[Day 22：实现 MCP Server](../../../docs/day-22-mcp-server.md)
- P0 默认只暴露只读 tools。
- 低风险写工具和高风险动作必须等安全审批边界落地后再开放。

## P0 Tools

Day 22 已实现以下只读工具：

| Tool | 风险级别 | 说明 |
| --- | --- | --- |
| `kb_search` | `READ_ONLY` | 按租户检索 FAQ、政策和产品知识 |
| `order_lookup` | `READ_ONLY` | 按租户和订单号查询订单摘要 |
| `course_catalog` | `READ_ONLY` | 按租户查询课程、政策和 FAQ 目录 |
| `refund_policy_check` | `READ_ONLY` | 检查退款政策并返回审批建议，不执行真实资金操作 |

## 暂不默认暴露

| Tool | 原因 |
| --- | --- |
| `handoff_to_human` | `LOW_RISK_WRITE`，会创建本地转人工记录，需要显式启用和审计 |
| `create_approval_request` | 需要先落审批模型和权限守卫 |
| `refund_execute` / `cancel_order` / `reschedule_order` | 高风险真实业务动作，MVP 默认禁止 |

## 模块边界

`customer-mcp-server` 复用 `customer-domain` 的工具契约和权限模型，但不应直接依赖 `customer-agent-app` 的 Controller 或启动类。Day 22 实现时应通过共享工具用例或 adapter 复用业务逻辑，避免复制订单、知识库和退款政策规则。

## 本地验证

```bash
mvn -pl customer-mcp-server -am test
```

如果本机没有全局 `mvn`，可使用 SDKMAN Maven 绝对路径：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/3.9.10/bin/mvn -pl customer-mcp-server -am test
```

MCP Inspector 示例：

```bash
mvn -pl customer-mcp-server -am package
npx @modelcontextprotocol/inspector \
  java -jar customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar
```
