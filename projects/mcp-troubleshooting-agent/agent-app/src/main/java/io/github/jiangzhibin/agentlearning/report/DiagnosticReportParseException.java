package io.github.jiangzhibin.agentlearning.report;

/**
 * 诊断报告解析异常。
 * <p>
 * 模型输出不满足结构化契约时抛出，方便上层决定是否重试或降级。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:57:18
 */
public class DiagnosticReportParseException extends RuntimeException {

    /**
     * 创建解析异常。
     *
     * @param message 错误信息
     */
    public DiagnosticReportParseException(String message) {
        super(message);
    }

    /**
     * 创建带原因的解析异常。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public DiagnosticReportParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
