package io.github.jiangzhibin.agentlearning.llm;

/**
 * 聊天模型调用入口。
 * <p>
 * 该接口隔离 Agent 应用层与具体模型供应商，当前只承载 Day 06 的最小文本问答能力。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:14:15
 */
public interface ChatModelClient {

    /**
     * 向模型发送用户问题并返回助手文本。
     *
     * @param userQuestion 用户输入的问题
     * @return 模型回复
     * @throws ChatModelException 模型调用失败、响应无法解析或调用超时时抛出
     */
    ChatModelResponse complete(String userQuestion);
}
