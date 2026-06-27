package com.example.customer.agent.chat;

/**
 * 未配置真实模型时的占位客户端。
 * <p>
 * 默认本地调试不调用外部模型；如果业务代码误调用该客户端，立即暴露配置问题。
 *
 * @author jiangzhibin
 * @since 2026-06-27 10:55:00
 */
public class DisabledCustomerChatModelClient implements CustomerChatModelClient {

    /**
     * 阻止未启用模型时的误调用。
     *
     * @param prompt 客服对话提示词上下文
     * @return 不会返回
     */
    @Override
    public String generateReply(CustomerChatPrompt prompt) {
        throw new ChatModelException("模型调用未启用", null);
    }
}
