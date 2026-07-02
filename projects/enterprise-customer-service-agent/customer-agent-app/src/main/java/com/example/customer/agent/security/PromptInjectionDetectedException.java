package com.example.customer.agent.security;

/**
 * Prompt Injection 检测异常。
 * <p>
 * 该异常表示用户输入试图覆盖系统指令或绕过高风险审批，后续工具调用必须停止。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
public class PromptInjectionDetectedException extends RuntimeException {

    private final PromptInspectionResult inspectionResult;

    /**
     * 创建 Prompt Injection 检测异常。
     *
     * @param inspectionResult 检查结果
     */
    public PromptInjectionDetectedException(PromptInspectionResult inspectionResult) {
        super(String.join("；", inspectionResult.reasons()) + "；message=" + inspectionResult.sanitizedMessage());
        this.inspectionResult = inspectionResult;
    }

    /**
     * 返回检查结果。
     *
     * @return 检查结果
     */
    public PromptInspectionResult inspectionResult() {
        return inspectionResult;
    }
}
