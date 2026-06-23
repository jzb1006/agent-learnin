package io.github.jiangzhibin.agentlearning.llm;

/**
 * 模型调用受控异常。
 * <p>
 * 上层只需要识别模型调用失败，不应暴露底层 HTTP Client 或敏感配置细节。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:14:15
 */
public class ChatModelException extends RuntimeException {

    /**
     * 创建模型调用异常。
     *
     * @param message 已脱敏的错误信息
     */
    public ChatModelException(String message) {
        super(message);
    }

    /**
     * 创建带原因的模型调用异常。
     *
     * @param message 已脱敏的错误信息
     * @param cause 原始异常
     */
    public ChatModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
