# Day 19：多租户隔离

## 目标

把 Day 12 到 Day 18 已经散落在订单、工具和 RAG 里的 `tenantId` 字段过滤，收敛为统一的运行时入口边界。

Day 19 的核心不是做租户后台，也不是接入登录态，而是建立一个稳定规则：

```text
业务接口必须从 X-Tenant-ID 解析当前租户，服务层和工具调用只使用当前租户访问数据。
```

## 业务场景

企业客服 Agent 面向多个业务租户。用户请求订单、退款政策或知识库答案时，不能因为 body 里伪造了 `tenantId`，或因为订单号相同，就读到其他租户的数据。

## 模块边界

| 模块 | 职责 |
| --- | --- |
| `tenant` | 解析 `X-Tenant-ID`，建立 `TenantContext` |
| `api` | 业务接口从租户上下文读取当前租户 |
| `order` | 订单查询使用 `orderId + tenantId` |
| `chat` | 工具调用参数使用请求头租户 |
| `rag` | 知识检索沿用 tenant filter |
| `customer-admin-web` | 调试台请求带 `X-Tenant-ID` |

## 接口设计

租户请求头：

```http
X-Tenant-ID: tenant-demo
```

当前强制租户头的接口：

```text
POST /chat
GET /api/orders/{orderId}
```

健康检查不需要租户：

```text
GET /health
```

`POST /chat` 的 body 仍保留 `tenantId` 字段用于兼容旧调试台，但运行时以 `X-Tenant-ID` 为准。

## 数据模型

Day 19 不新增数据库表。

当前隔离依赖：

| 数据 | 隔离方式 |
| --- | --- |
| mock 订单 | `MockOrderRepository.findByIdAndTenantId(orderId, tenantId)` |
| 工具调用 | `toolCall.arguments.tenantId` 使用请求头租户 |
| 知识库 | Spring AI `VectorStore` filter：`tenant = currentTenantId` |

## 安全边界

- 缺少 `X-Tenant-ID` 返回 `TENANT_REQUIRED`。
- 租户头只能包含字母、数字、点、下划线、冒号和连字符，非法值返回 `TENANT_INVALID`。
- `GET /api/orders/{orderId}` 不再使用只按订单号查询的入口。
- `/chat` 不信任 body 里的 `tenantId`，防止 body/header 租户不一致时越权。
- 跨租户订单返回 `ORDER_NOT_FOUND`，不暴露订单真实归属。

## 验证方式

后端：

```bash
mvn -pl customer-agent-app -am -Dtest=CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

前端：

```bash
npm test
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 缺少 `X-Tenant-ID` 查询订单 | 400，`TENANT_REQUIRED` |
| 非法 `X-Tenant-ID` 查询订单 | 400，`TENANT_INVALID` |
| `tenant-other` 查询 `tenant-demo` 订单 | 404，`ORDER_NOT_FOUND` |
| `/chat` body 租户与 header 不一致 | 工具调用使用 header 租户 |
| `/chat` body 不传 `tenantId` | 只要 header 合法即可处理 |
| 调试台订单和 chat 请求 | 均携带 `X-Tenant-ID` |

## 原则应用

- KISS：只引入 `TenantContext`、`TenantResolver` 和 MVC 拦截器，不做租户管理后台。
- YAGNI：不引入登录态、RBAC、租户配置表或跨租户运营后台。
- DRY：租户头解析和格式校验集中在 `TenantResolver`。
- SOLID：租户解析、API 入口、订单查询、RAG 检索各自保持单一职责。
