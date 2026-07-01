package com.example.customer.agent.chat;

/**
 * 客服 Agent Prompt 模板。
 * <p>
 * 集中维护模型行为契约和版本号，避免 Prompt 文本散落在模型适配层中。
 *
 * @author jiangzhibin
 * @since 2026-06-27 15:05:00
 */
public class CustomerChatPromptTemplate {

    private static final String VERSION = "customer-agent-prompt-v1";

    private static final String SYSTEM_PROMPT = """
            你是企业级智能客服与订单协同 Agent。

            行为契约：
            1. 不得编造订单状态、履约进度、课程时间、退款结果或任何未提供的业务事实。
            2. 不得承诺真实退款成功、真实取消订单成功或真实改签成功。
            3. 必须基于工具、知识库或用户输入证据生成回复；证据不足时说明需要继续确认或转人工。
            4. 高风险动作必须进入审批，不得绕过人工确认直接执行。
            5. 回复保持简洁、专业，面向客服场景给出下一步建议。
            6. 只返回 JSON object，不要输出 Markdown、代码块或额外解释。

            JSON 字段：
            - route：必须照抄事实证据中的 route。
            - answer：面向用户的客服回复。
            - sources：必须照抄事实证据中的 sources；没有证据时返回空数组。
            - riskLevel：必须照抄事实证据中的 riskLevel。
            - nextActions：字符串数组，给出下一步建议。
            - traceId：必须照抄事实证据中的 traceId；服务端会用请求 traceId 覆盖该字段。
            """;

    /**
     * 返回 Prompt 版本号。
     *
     * @return Prompt 版本号
     */
    public String version() {
        return VERSION;
    }

    /**
     * 返回系统提示词。
     *
     * @return 系统提示词
     */
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 渲染用户提示词。
     *
     * @param prompt 客服对话提示词上下文
     * @return 用户提示词
     */
    public String userPrompt(CustomerChatPrompt prompt) {
        return """
                Prompt 版本：%s
                租户：%s
                用户问题：%s
                会话摘要：%s
                事实证据：%s
                """.formatted(
                version(),
                prompt.tenantId(),
                prompt.message(),
                prompt.memorySummary().isBlank() ? "无" : prompt.memorySummary(),
                prompt.orderEvidence());
    }
}
