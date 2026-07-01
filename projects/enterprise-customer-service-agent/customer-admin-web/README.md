# customer-admin-web

本地 Agent 调试台，固定技术栈：

- Vite
- React
- TypeScript
- Ant Design
- TanStack Query

Day 04 已接入基础 API 调试快照：

- Health：展示 `GET /health` 状态。
- Order Debug：展示 `GET /api/orders/order-1001` 结果。
- Chat Console：展示 `POST /chat` 的 route、riskLevel、traceId、conversationId、memorySummary、answer、sources 和 nextActions。
- Memory Debug：通过固定会话 ID 连续发送“刚才那个订单”这类追问，验证 Day 24 短期上下文。

后续逐步加入：

- Chat Console
- Request Inspector
- Tool Calls
- RAG Sources
- Order Debug
- Approval Debug
- Health

## 命令

```bash
npm test
npm run build
npm run dev
```

开发代理默认转发到 `http://127.0.0.1:8080`，可通过 `CUSTOMER_AGENT_API_BASE_URL` 覆盖。联调时先启动 `customer-agent-app`：

```bash
cd ..
mvn -pl customer-agent-app -am package -DskipTests
java -jar customer-agent-app/target/customer-agent-app-0.1.0-SNAPSHOT.jar
```

如果 8080 已被其他本地进程占用，可以改用独立端口：

```bash
cd ..
java -jar customer-agent-app/target/customer-agent-app-0.1.0-SNAPSHOT.jar --server.port=18080

cd customer-admin-web
CUSTOMER_AGENT_API_BASE_URL=http://127.0.0.1:18080 npm run dev -- --port 5174
```
