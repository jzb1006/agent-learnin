# ADR：Day 10 本地 Git 历史与配置读取工具边界

## 背景

Day 09 已实现 `search_code`，排障 Agent 可以从源码中收集证据。

Day 10 需要补齐两个常见排障证据来源：

- Git 历史：确认近期提交和变更线索。
- 配置文件：确认运行参数、开关和连接信息。

这两个能力都可能暴露敏感信息，因此必须在本地工具层先完成访问边界和脱敏。

## 决策

在 `agent-app` 的 `tool` 包中新增：

- `LocalGitHistoryTool`
- `LocalConfigReadTool`
- `SensitiveValueRedactor`

`LocalGitHistoryTool` 的边界：

- 工具名为 `git_history`。
- 只接受 `keyword` 和 `maxResults`。
- `keyword` 最大 128 字符。
- `maxResults` 范围为 1-10，默认 5。
- 只查询 `allowedRoot` 自身的 `.git` 仓库。
- 不从子目录向上查找父级仓库。
- 提交信息返回前先脱敏。

`LocalConfigReadTool` 的边界：

- 工具名为 `read_config`。
- 只接受 `path`。
- `path` 必须是允许根目录内的相对路径。
- 只支持 `.properties`、`.yml`、`.yaml`、`.env`、`.conf`。
- 拒绝路径穿越和符号链接。
- 最多返回前 120 行。
- 每行返回前先脱敏。

## 备选方案

### 方案 A：使用 JDK + Git CLI 实现最小只读工具

Java 负责路径校验、参数校验和脱敏；Git 历史查询交给本机 `git log`。

### 方案 B：用 JGit 完成 Git 历史查询

完全在 Java 进程内读取 Git 对象，避免依赖本机 Git CLI。

### 方案 C：把 Git 和配置读取直接做到 MCP Server

跳过本地工具阶段，直接进入 MCP 工具化。

## 取舍

选择方案 A。

原因：

- KISS：当前教学目标是理解本地 Tool Calling 执行模型，不提前扩大到 MCP。
- YAGNI：JGit 会增加依赖和 API 学习成本，Day 10 不需要。
- DRY：Git 和配置都复用 `SensitiveValueRedactor`，避免各自实现脱敏规则。
- SOLID：工具实现只负责证据读取和边界保护，不承担 Agent 推理和诊断结论。

方案 B 在生产环境更便于消除外部命令依赖，但不是今天的最小闭环。方案 C 属于 Day 14 的学习内容，提前进入会模糊本地工具和 MCP 工具的边界。

## 后果

正向后果：

- Agent 可以从源码、Git 历史和配置三类证据收集排障上下文。
- 敏感值在进入模型上下文前已经脱敏。
- 路径边界、符号链接和工具参数都有测试覆盖。
- 两个新工具仍保持统一 `ToolResult` 返回格式。

代价：

- `git_history` 依赖本机安装 `git`。
- `git_history` 当前只看提交信息，不看 diff。
- `read_config` 只支持常见文本配置格式。
- 脱敏规则是启发式的，不能替代生产级秘密扫描器。

## 复查条件

出现以下情况时复查该决策：

- 运行环境没有 Git CLI，或需要跨平台无外部进程运行。
- 需要分析 Git diff、文件变更列表或 blame。
- 配置来源变为配置中心、数据库或远程服务。
- 敏感值类型增多，需要专门的 secret scanning 策略。
- Day 14 将 `git_history` 和 `read_config` 迁移到 MCP Server。
