# Day 20：知识库管理 API

## 目标

为 Day 16-19 已建立的知识源、RAG、pgvector 底座和多租户上下文补上显式管理入口：

```text
POST   /admin/api/v1/knowledge/items
GET    /admin/api/v1/knowledge/items
GET    /admin/api/v1/knowledge/search?query=...&topK=3
DELETE /admin/api/v1/knowledge/items?itemId=...
POST   /admin/api/v1/knowledge/reindex
```

Day 20 只拆管理面 API namespace，并补齐调试台需要的运行态列表、搜索和单条删除入口。不做完整知识库运营后台，不做分页、发布流、审核流、RBAC，不拆独立服务，不执行远程 DDL，不连接远程服务器。

## 业务场景

运营或开发者在调试企业客服 Agent 时，需要临时新增 FAQ、政策或产品知识，通过列表确认当前运行态已索引条目，通过搜索验证 RAG 命中效果。删除某条知识后，该知识不应继续被当前租户检索命中。应用启动只建立连接和 Bean，不再自动删除并重建全部向量数据。

## 模块边界

| 模块 | 职责 |
| --- | --- |
| `api` | 暴露管理侧知识新增、列表、搜索、删除和显式重建接口 |
| `knowledge` | 将 HTTP 请求转换为知识管理用例 |
| `rag` | 增量索引、运行态索引目录、搜索、删除索引、显式重建和内容指纹跳过 |
| `customer-admin-web` | 提供本地 Knowledge Debug 面板，覆盖保存、列表、搜索、删除和重建 |

## 接口设计

新增或更新知识：

```http
POST /admin/api/v1/knowledge/items
X-Tenant-ID: tenant-demo
Content-Type: application/json

{
  "itemId": "faq-day20-api",
  "category": "FAQ",
  "title": "Day20 知识管理 API",
  "content": "新增知识后，无需重启服务即可被 RAG 检索命中。",
  "source": "day20#api",
  "version": "2026-06-30",
  "tags": ["day20", "knowledge"]
}
```

响应：

```json
{
  "itemId": "faq-day20-api",
  "tenantId": "tenant-demo",
  "indexedChunks": 1,
  "skipped": false
}
```

查看当前运行态已索引知识：

```http
GET /admin/api/v1/knowledge/items
X-Tenant-ID: tenant-demo
```

响应：

```json
{
  "items": [
    {
      "itemId": "faq-day20-api",
      "tenantId": "tenant-demo",
      "category": "FAQ",
      "title": "Day20 知识管理 API",
      "source": "day20#api",
      "version": "2026-06-30",
      "indexedChunks": 1,
      "contentPreview": "新增知识后，无需重启服务即可被 RAG 检索命中。"
    }
  ]
}
```

搜索当前租户知识：

```http
GET /admin/api/v1/knowledge/search?query=知识库管理&topK=3
X-Tenant-ID: tenant-demo
```

响应：

```json
{
  "query": "知识库管理",
  "tenantId": "tenant-demo",
  "topK": 3,
  "matches": [
    {
      "itemId": "faq-day20-api",
      "title": "Day20 知识管理 API",
      "source": "day20#api",
      "tenant": "tenant-demo",
      "category": "FAQ",
      "content": "新增知识后，无需重启服务即可被 RAG 检索命中。",
      "score": 0.91
    }
  ]
}
```

删除知识索引：

```http
DELETE /admin/api/v1/knowledge/items?itemId=faq-day20-api
X-Tenant-ID: tenant-demo
```

显式重建本地知识库索引：

```http
POST /admin/api/v1/knowledge/reindex
X-Tenant-ID: tenant-demo
```

## 数据模型

Day 20 不新增数据库表。

知识列表来自 `KnowledgeRetrievalService` 维护的当前运行态索引目录，用于本地调试和验收，不等同于持久化运营知识表。

向量 chunk 的关键 metadata：

| 字段 | 用途 |
| --- | --- |
| `tenant` | 租户过滤 |
| `itemId` | 单条知识删除和重复索引判断 |
| `source` | RAG 来源引用 |
| `category` | FAQ / POLICY / PRODUCT 分类 |
| `version` | 内容版本 |

## 安全边界

- 所有 `/admin/api/v1/knowledge/**` 接口必须携带合法 `X-Tenant-ID`。
- 知识库管理属于 control-plane，不与 `/chat`、`/api/orders/**` 等用户侧 runtime API 共用 namespace。
- 新增知识的租户归属只取请求头，不信任 body。
- 列表和搜索只返回当前请求租户的数据，不能跨租户泄露知识条目。
- 删除只删除当前租户下指定 `itemId` 的向量 chunk，不删除仓库里的 Markdown 源文件。
- 重建索引是显式 API 动作，应用启动不会自动清空并重建向量表。
- 远程 pgvector DDL 仍只允许人工确认后执行。

## 验证方式

后端定向测试：

```bash
/Users/jiangzhibin/.sdkman/candidates/maven/current/bin/mvn -q -pl customer-agent-app -am -Dtest=KnowledgeRetrievalServiceTest,CustomerAgentApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

前端测试：

```bash
npm test -- --run
```

## 测试用例

| 用例 | 预期 |
| --- | --- |
| 构造 `KnowledgeRetrievalService` | 不自动调用 `VectorStore.add/delete` |
| 新增知识项后检索 | 当前租户无需重启即可命中 |
| 删除知识项后检索 | 当前租户不再命中该知识 |
| 列出知识项 | 只返回当前运行态已索引且属于当前租户的知识摘要 |
| 搜索知识项 | 复用 RAG 检索并返回 itemId、来源、分类和 score |
| 重复新增未变化知识 | 返回 `skipped=true`，不重复写入向量 chunk |
| 显式 reindex | 返回读取文档数、写入 chunk 数和跳过条目数 |
| Web 调试台 | 可保存知识、刷新列表、搜索知识、删除知识并触发重建索引 |

## 原则应用

- KISS：API 只覆盖新增、列表、搜索、删除和重建这几个 Day 20 调试闭环必需动作。
- YAGNI：列表只做运行态调试目录，不做持久化运营知识表、分页、审批、发布流、RBAC 或完整运营后台。
- DRY：所有向量写入和删除统一收敛到 `KnowledgeRetrievalService`。
- SOLID：`KnowledgeAdminController` 处理管理侧 HTTP，`KnowledgeManagementService` 处理用例编排，RAG 层处理索引细节。
