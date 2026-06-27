package com.example.customer.agent.chat;

/**
 * 客服对话模型客户端。
 * <p>
 * 业务层只依赖该接口，避免直接绑定 Spring AI 或具体模型供应商。
 *
 * @author jiangzhibin
 * @since 2026-06-27 10:55:00
 */
public interface CustomerChatModelClient {

    /**
     * 根据客服上下文生成回复。
     *
     * @param prompt 客服对话提示词上下文
     * @return 模型回复
     */
    String generateReply(CustomerChatPrompt prompt);
}
