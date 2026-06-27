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
- Chat Console：展示 `POST /chat` 的 route、riskLevel、traceId、reply 和 nextActions。

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

开发代理默认转发到 `http://127.0.0.1:8080`，本地联调时先启动 `customer-agent-app`：

```bash
cd ..
mvn -pl customer-agent-app -am package -DskipTests
java -jar customer-agent-app/target/customer-agent-app-0.1.0-SNAPSHOT.jar
```
