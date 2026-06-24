# ADR：Day 12 最小 MCP Server 边界

## 背景

Day 11 已确定 `agent-app` 作为 MCP Host，`mcp-server` 作为 MCP Server。Day 12 需要把这个边界落成可运行代码，但还不应该迁移真实排障工具。

如果一开始就迁移 `search_code`、`git_history` 或 `read_config`，学习焦点会从 MCP 协议闭环转移到路径校验、Git 命令、配置脱敏和结果序列化，超出当天目标。

## 决策

Day 12 只实现一个 MCP Tool：

```text
ping -> pong
```

具体决策：

- `mcp-server` 使用 MCP Java SDK。
- transport 采用 stdio。
- `ping` 不接收参数，输入 schema 是空对象。
- `ping` 返回文本 `pong`。
- `ping` 标记为只读、幂等、非破坏性。
- `mcp-server` 通过 Maven Shade Plugin 生成可执行 JAR。
- `agent-app` 新增 `MinimalMcpClient`，只支持 `listTools()` 和无参文本工具调用。
- `agent-app` 的 Jackson 2 依赖升级到 `2.20.0`，避免覆盖 MCP SDK 需要的 `jackson-annotations:2.20`。

## 备选方案

### 方案 A：只写 Server 单元测试，不做 Agent 侧 stdio 集成

可以验证工具注册，但不能证明 Agent 能通过 MCP Client 连接 Server。

### 方案 B：用 stdio MCP Server + Agent 侧 Client 集成测试

完整覆盖 `initialize -> tools/list -> tools/call`。

### 方案 C：直接做 Streamable HTTP MCP Server

更接近远程部署，但需要提前处理 HTTP 生命周期、端口、鉴权和跨客户端连接。

## 取舍

选择方案 B。

原因：

- KISS：`ping` 最小，stdio 最贴近本地开发。
- YAGNI：当前不需要远程多客户端和认证。
- DRY：后续工具迁移复用同一 Client 和 Server 启动方式。
- SOLID：Agent 侧只依赖 MCP Client，工具实现留在 Server。

## 后果

正向后果：

- Day 12 有真实协议级验收，而不是只验证 Java 方法。
- Day 13 可以专注迁移 `search_code`，不用重新搭 MCP 启动链路。
- Server stderr 被 Client 收集，后续排查 JAR 启动失败更直接。

代价：

- 新增 MCP SDK 依赖。
- 新增可执行 JAR 打包配置。
- `agent-app` 的 Jackson 2 版本需要收敛到 `2.20.0`。
- 当前 MCP SDK stdio 相关 API 会产生过时 API 编译提示，后续升级 SDK 时需要复查。

## 复查条件

出现以下情况时复查该决策：

- MCP Java SDK 推荐的 stdio API 发生替换。
- 需要把 MCP Server 部署成远程服务。
- 需要多 Agent 共享同一个 Server。
- `mcp-server` 的 shaded JAR 体积或依赖冲突影响开发体验。
- Day 20 建立 contract test 后发现当前最小 Client 抽象不足。
