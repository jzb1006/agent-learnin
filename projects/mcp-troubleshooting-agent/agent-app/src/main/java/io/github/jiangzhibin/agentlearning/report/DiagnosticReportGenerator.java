package io.github.jiangzhibin.agentlearning.report;

import io.github.jiangzhibin.agentlearning.llm.ChatModelClient;
import lombok.RequiredArgsConstructor;

/**
 * 结构化诊断报告生成器。
 * <p>
 * 该类组合模型文本调用和结构化解析，不关心具体模型供应商。
 *
 * @author jiangzhibin
 * @since 2026-06-23 16:00:05
 */
@RequiredArgsConstructor
public class DiagnosticReportGenerator {

    private static final String REPORT_SCHEMA = """
        请根据用户排障问题生成结构化诊断报告。
        只输出一个 JSON 对象，不要输出 Markdown、解释文字或代码块。
        JSON schema：
        {
          "summary": "string，简短诊断摘要",
          "evidence": ["string，已知证据或需要核验的证据"],
          "nextActions": ["string，只读后续排查动作"],
          "riskLevel": "LOW | MEDIUM | HIGH"
        }
        """;

    private final ChatModelClient chatModelClient;
    private final DiagnosticReportParser parser;
    private final int maxAttempts;

    /**
     * 生成结构化诊断报告。
     *
     * @param userQuestion 用户排障问题
     * @return 结构化诊断报告
     */
    public DiagnosticReport generate(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("用户问题不能为空");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts 必须大于 0");
        }

        DiagnosticReportParseException lastException = null;
        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            var prompt = buildPrompt(userQuestion, lastException);
            var response = chatModelClient.complete(prompt);
            try {
                return parser.parse(response.content());
            } catch (DiagnosticReportParseException parseException) {
                lastException = parseException;
            }
        }

        throw new DiagnosticReportParseException(
            "模型输出无法解析为诊断报告，attempts=" + maxAttempts,
            lastException
        );
    }

    private String buildPrompt(String userQuestion, DiagnosticReportParseException lastException) {
        if (lastException == null) {
            return REPORT_SCHEMA + "\n用户问题：\n" + userQuestion;
        }
        return REPORT_SCHEMA
            + "\n上一次输出解析失败，错误："
            + lastException.getMessage()
            + "\n请严格按 schema 重新输出。\n用户问题：\n"
            + userQuestion;
    }
}
