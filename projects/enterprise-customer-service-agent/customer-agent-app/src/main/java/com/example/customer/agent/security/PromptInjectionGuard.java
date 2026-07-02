package com.example.customer.agent.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Prompt Injection 检查器。
 * <p>
 * 只做确定性高风险短语拦截，防止用户输入覆盖系统指令或诱导高风险工具绕过审批。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
@Component
public class PromptInjectionGuard {

    private static final Pattern OVERRIDE_INSTRUCTION = Pattern.compile(
            "(?iu)(忽略|无视|忘记|覆盖).{0,12}(规则|指令|系统|system|developer|instruction|prompt)|ignore\\s+(all\\s+)?(previous|prior)\\s+(instructions|rules)");
    private static final Pattern BYPASS_APPROVAL = Pattern.compile(
            "(?iu)(不要|无需|不用|跳过|绕过).{0,8}(审批|人工|复核)|bypass\\s+approval|without\\s+approval|直接(退款|取消|改签)");

    private final RedactionService redactionService;

    /**
     * 创建默认 Prompt Injection 检查器。
     */
    public PromptInjectionGuard() {
        this(new RedactionService());
    }

    /**
     * 创建 Prompt Injection 检查器。
     *
     * @param redactionService 脱敏服务
     */
    public PromptInjectionGuard(RedactionService redactionService) {
        this.redactionService = redactionService;
    }

    /**
     * 检查用户消息是否包含提示注入。
     *
     * @param message 用户消息
     * @return 检查结果
     */
    public PromptInspectionResult inspect(String message) {
        var redactedMessage = redactionService.redact(message);
        var reasons = new ArrayList<String>();
        var sanitizedMessage = redactedMessage;

        if (OVERRIDE_INSTRUCTION.matcher(redactedMessage).find()) {
            reasons.add("检测到覆盖系统指令的提示注入");
            sanitizedMessage = OVERRIDE_INSTRUCTION.matcher(sanitizedMessage).replaceAll("[PROMPT_INJECTION_REMOVED]");
        }
        if (BYPASS_APPROVAL.matcher(redactedMessage).find()) {
            reasons.add("检测到绕过审批的高风险动作要求");
            sanitizedMessage = BYPASS_APPROVAL.matcher(sanitizedMessage).replaceAll("[PROMPT_INJECTION_REMOVED]");
        }

        if (reasons.isEmpty()) {
            return new PromptInspectionResult(true, "LOW", List.of(), redactedMessage);
        }
        return new PromptInspectionResult(false, "HIGH", reasons, sanitizedMessage);
    }

    /**
     * 检查消息，不安全时抛出异常。
     *
     * @param message 用户消息
     * @return 可继续用于后续链路的脱敏消息
     */
    public String requireSafeMessage(String message) {
        var result = inspect(message);
        if (!result.safe()) {
            throw new PromptInjectionDetectedException(result);
        }
        return result.sanitizedMessage();
    }
}
