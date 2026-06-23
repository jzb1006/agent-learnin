# mcp-server

`mcp-server` 负责把排障能力标准化暴露给 Agent。

## 负责

- 暴露只读 MCP Tools，例如 `search_code`、`read_config`、`git_history`。
- 暴露 MCP Resources，例如项目元信息、接口文档摘要、配置摘要。
- 暴露 MCP Prompts，例如常见排障流程模板。
- 管理 MCP Roots 和允许访问的目标项目目录。
- 对工具输入做校验，对敏感配置做脱敏。
- 返回结构化、可追溯、可裁剪的工具结果。

## 不负责

- 不决定最终诊断结论。
- 不把自然语言业务推理写进工具实现。
- 不执行默认写操作。
- 不通过路径穿越读取允许根目录外的文件。

## MCP 建模规则

- 主动查询或计算：建模为 Tool。
- 稳定只读上下文：建模为 Resource。
- 可复用交互流程：建模为 Prompt。
- 访问目录边界：建模为 Roots 和服务端校验。
