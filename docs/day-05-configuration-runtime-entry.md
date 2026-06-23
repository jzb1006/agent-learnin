# Day 05：配置管理与运行入口设计

## 今日节点

Day 05：配置管理与运行入口设计。

## 今日主题

为 MCP 优先的 Java 后端排障 Agent 设计第一版配置结构、环境变量边界、只读根目录约束，以及 CLI / REST 两种运行入口草案。

## 今天解决的问题

- Agent 应该配置哪些运行参数。
- 哪些值可以进仓库，哪些值只能来自环境变量或本地私有配置。
- 目标项目路径和只读根目录为什么必须可配置。
- CLI 与 REST 入口分别适合什么场景。
- 如何在不实现 Day 06 LLM API 的前提下，为后续代码落地保留清晰契约。

## 今天不解决的问题

- 不接入真实 LLM API。
- 不创建 Spring Boot 工程。
- 不实现 CLI 或 REST Controller。
- 不实现 MCP Client / Server 连接。
- 不实现 Tool Calling、RAG、Memory 或 Agent 编排。
- 不写入真实 API Key、token、password 或生产环境地址。
- 不执行重启、部署、改配置、写数据库或生产 API 调用。

## 一句话定义

配置管理是把“运行时会变、环境相关、敏感或用户可选择”的信息从代码里剥离出来，并通过明确的默认值、环境变量和本地 profile 控制 Agent 能访问什么、如何运行、失败时如何定位。

## 为什么 Day 05 先做配置设计

排障 Agent 后面会接触模型、目标项目、MCP Server、知识库、trace 和评测样例。

如果这些内容一开始写死，后续会出现几个问题：

- API Key 可能被误提交。
- 目标项目路径无法切换。
- Agent 可能越过只读根目录读取无关文件。
- CLI、REST、测试用例各自定义参数，导致行为不一致。
- 排查失败时无法判断是模型问题、配置问题还是路径权限问题。

所以 Day 05 先定义配置契约，再进入 Day 06 的最小 LLM API 调用。

## 配置分层

| 层级 | 用途 | 是否进仓库 | 示例 |
| --- | --- | --- | --- |
| 默认配置 | 非敏感默认行为和本地开发约定 | 可以 | 超时时间、最大轮数、默认输出格式 |
| 本地 profile | 个人机器上的开发覆盖项 | 不提交真实文件，只提交 `.example` | 本地目标项目路径、本地模型 provider |
| 环境变量 | 敏感值和部署时变化的值 | 不提交 | `DEEPSEEK_API_KEY`、模型 base url |
| 命令行参数 | 单次运行覆盖项 | 不持久化 | 用户问题、输出文件、目标项目路径 |
| REST 请求参数 | Web / 服务化入口的一次性输入 | 不持久化 | 用户问题、trace 开关、报告格式 |

核心原则：

- 敏感值只来自环境变量或未提交的本地私有文件。
- 目标项目路径必须显式配置，不能写死在代码里。
- 允许访问的只读根目录必须由配置声明，并由工具层二次校验。
- CLI 和 REST 入口复用同一套应用配置对象，避免行为分叉。

## 第一版配置结构草案

后续创建 Spring Boot 工程时，可以把配置映射成 `@ConfigurationProperties`。

当前只定义结构，不创建真实配置文件：

```yaml
agent:
  name: mcp-troubleshooting-agent
  mode: readonly
  max-steps: 5
  output-format: markdown

model:
  provider: deepseek
  base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
  api-key: ${DEEPSEEK_API_KEY:}
  model-name: ${DEEPSEEK_MODEL:deepseek-v4-flash}
  timeout-seconds: 30

target-project:
  name: sample-java-service
  root-path: ${TARGET_PROJECT_ROOT:}
  readonly-roots:
    - ${TARGET_PROJECT_ROOT:}
  exclude-patterns:
    - .git
    - target
    - build
    - node_modules

mcp:
  server:
    enabled: false
    transport: stdio
    command: ${MCP_SERVER_COMMAND:}

knowledge-base:
  root-path: ./knowledge-base
  max-snippets: 5

trace:
  enabled: true
  output-dir: ./traces
  redact-sensitive-values: true

safety:
  readonly-only: true
  require-confirmation-for-write-actions: true
  blocked-actions:
    - restart_service
    - trigger_deploy
    - update_config
    - write_database
    - call_production_api
```

## 环境变量草案

| 环境变量 | 用途 | 是否敏感 | 是否必填 | 说明 |
| --- | --- | --- | --- | --- |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | 是 | Day 06 后必填 | 不写入仓库，不输出到日志 |
| `DEEPSEEK_BASE_URL` | DeepSeek OpenAI-compatible 服务地址 | 可能 | 否 | 默认使用 `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | DeepSeek 模型名称 | 否 | 否 | 默认测试使用 `deepseek-v4-flash`，复杂推理可切换为 `deepseek-v4-pro` |
| `TARGET_PROJECT_ROOT` | 被排障 Java 项目根目录 | 否 | 是 | 必须指向允许读取的本地目录 |
| `MCP_SERVER_COMMAND` | MCP Server 启动命令 | 可能 | Day 12 后需要 | 当前阶段先保留 |

DeepSeek 当前提供 OpenAI-compatible API，因此后续 Java 代码可以保留“OpenAI-compatible Chat Client”这类抽象，但配置命名以 DeepSeek 为准。模型名称属于外部服务契约，Day 06 实现前应再通过官方 `/models` 或文档确认一次。

敏感值判断规则：

- 名称包含 `key`、`token`、`secret`、`password`、`credential` 的值默认敏感。
- 敏感值不能进入日志、trace、eval case 或诊断报告。
- 工具读取配置时只能返回脱敏摘要。

## 只读根目录设计

`target-project.root-path` 表示当前排障目标项目。

`target-project.readonly-roots` 表示工具允许读取的根目录集合。第一阶段建议只允许一个根目录，避免过早支持多项目排障。

路径校验必须遵守：

1. 用户输入路径先做规范化。
2. 规范化后的路径必须位于 `readonly-roots` 内。
3. 默认排除 `.git` 内部对象、构建产物和依赖目录。
4. 符号链接不能绕过只读根目录。
5. 任何越界访问都返回明确错误，不交给模型自由判断。

这条规则后续主要由 `mcp-server` 的工具实现负责，`agent-app` 不能绕过 MCP Server 自行读取目标项目。

## CLI 入口草案

CLI 适合本地开发、学习验证和一次性排障。

目标形态：

```bash
java -jar agent-app.jar diagnose \
  --question "登录接口返回 500，帮我只读排查" \
  --target-root "/path/to/java-service" \
  --output "report.md" \
  --format markdown
```

入口参数：

| 参数 | 用途 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `--question` | 用户故障描述 | 是 | 后续进入 Agent 输入 |
| `--target-root` | 目标项目路径 | 是 | 可覆盖 `TARGET_PROJECT_ROOT` |
| `--output` | 报告输出路径 | 否 | 默认输出到终端 |
| `--format` | 报告格式 | 否 | 第一版只支持 `markdown` |
| `--trace` | 是否写 trace | 否 | 默认读取配置 |

CLI 的第一阶段边界：

- 只触发只读诊断。
- 不提供 `restart`、`deploy`、`update-config` 等子命令。
- 不接受明文 API Key 参数，DeepSeek API Key 只能来自环境变量或本地私有配置。

## REST 入口草案

REST 适合后续接 Web UI、平台集成或服务化调用。

目标形态：

```http
POST /api/diagnostics
Content-Type: application/json

{
  "question": "登录接口返回 500，帮我只读排查",
  "targetProjectRoot": "/path/to/java-service",
  "outputFormat": "markdown",
  "traceEnabled": true
}
```

响应草案：

```json
{
  "traceId": "20260623-001",
  "status": "completed",
  "riskLevel": "unknown",
  "reportMarkdown": "# 诊断报告...",
  "blockedActions": [
    "restart_service",
    "trigger_deploy",
    "update_config",
    "write_database",
    "call_production_api"
  ]
}
```

REST 的第一阶段边界：

- 只提供诊断接口，不提供写操作接口。
- 请求中的 `targetProjectRoot` 必须经过只读根目录校验。
- 响应中不返回敏感配置明文。
- 错误响应要区分配置缺失、路径越界、模型失败、工具失败和报告生成失败。

## CLI 与 REST 的取舍

| 入口 | 优点 | 缺点 | 当前定位 |
| --- | --- | --- | --- |
| CLI | 本地验证简单，适合学习和脚本化 | 不适合多人服务化使用 | 第一阶段优先设计 |
| REST | 易接 Web UI 和平台系统 | 需要考虑鉴权、并发、部署和安全 | 先定义草案，后期实现 |

Day 05 的决策是：先以 CLI 作为最小运行入口方向，同时保留 REST 契约草案。真正实现入口放到 Day 41；Day 06-10 仍优先补模型调用、结构化输出和只读工具。

## 项目映射

```text
配置结构
  -> agent-app
    -> 模型配置
    -> Agent 运行参数
    -> trace 开关

只读根目录
  -> mcp-server
    -> Tools 路径校验
    -> Resources 访问边界

知识库路径
  -> knowledge-base

评测输入
  -> evals
    -> 可覆盖 question / target root / expected report shape

运行记录
  -> traces
    -> 不保存敏感值
```

## KISS / YAGNI / DRY / SOLID 落地

KISS：只定义一个 `diagnose` 核心入口，不设计复杂命令组。

YAGNI：不提前实现鉴权、多租户、多 Agent、多项目并行排障。

DRY：CLI、REST、测试都复用同一套配置结构，不各自发明参数。

SOLID：配置对象只表达运行环境和边界；Agent 编排、工具实现和报告生成仍保持职责分离。

## 验收标准

完成 Day 05 时，你应该能解释：

- 为什么 API Key 不能写入仓库。
- 为什么目标项目路径必须可配置。
- 为什么只读根目录要在工具层再次校验。
- CLI 和 REST 入口分别适合什么场景。
- 为什么 Day 05 不应该顺手实现真实模型调用。

## 复习问题

1. `target-project.root-path` 和 `target-project.readonly-roots` 有什么区别？
2. 为什么不建议通过 CLI 参数传入 `DEEPSEEK_API_KEY`？
3. 如果用户请求读取 `/etc/hosts`，但只读根目录是某个 Java 项目，Agent 应该怎么处理？
4. CLI 和 REST 为什么应该复用同一套配置对象？
5. Day 05 只做入口草案，不实现真实入口，体现了哪个工程原则？
