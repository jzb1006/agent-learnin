# customer-agent-app

Spring Boot 应用入口模块，后续负责：

- `/chat`
- `/health`
- 订单查询 API
- Agent 编排
- Trace 写入
- 调试台 API

Day 04 已实现基础 REST API：

| 接口 | 当前行为 |
| --- | --- |
| `GET /health` | 返回 `status=UP` 和 `service=customer-agent-app` |
| `GET /api/orders/{orderId}` | 返回内存 mock 订单；不存在时返回 `ORDER_NOT_FOUND` |
| `POST /chat` | 用确定性规则返回 `ORDER_LOOKUP`、`READ_ONLY`、订单证据和下一步动作 |

当前边界：

- 不接真实数据库。
- 不调用真实 LLM。
- 不调用 MCP Server。
- 不执行真实退款、取消或改签。

## 验证

```bash
cd ..
mvn -pl customer-agent-app -am test -Dtest=CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false
```

本地启动：

```bash
cd ..
mvn -pl customer-agent-app -am package -DskipTests
java -jar customer-agent-app/target/customer-agent-app-0.1.0-SNAPSHOT.jar
```
