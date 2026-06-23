package io.github.jiangzhibin.agentlearning.report;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 诊断报告 JSON 解析器。
 * <p>
 * 只负责把模型输出解析并校验为 {@link DiagnosticReport}，不负责调用模型或生成重试策略。
 *
 * @author jiangzhibin
 * @since 2026-06-23 15:57:18
 */
public class DiagnosticReportParser {

    private final ObjectMapper objectMapper;

    /**
     * 使用默认 ObjectMapper 创建解析器。
     */
    public DiagnosticReportParser() {
        this(new ObjectMapper());
    }

    /**
     * 创建诊断报告解析器。
     *
     * @param objectMapper JSON 解析器
     */
    public DiagnosticReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析模型输出的 JSON 诊断报告。
     *
     * @param json 模型输出 JSON
     * @return 诊断报告
     * @throws DiagnosticReportParseException JSON 无效或字段校验失败时抛出
     */
    public DiagnosticReport parse(String json) {
        if (json == null || json.isBlank()) {
            throw new DiagnosticReportParseException("诊断报告 JSON 不能为空");
        }

        try {
            return objectMapper.readValue(json, DiagnosticReport.class);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new DiagnosticReportParseException(illegalArgumentException.getMessage(), illegalArgumentException);
        } catch (IOException ioException) {
            throw new DiagnosticReportParseException("诊断报告 JSON 解析失败：" + ioException.getMessage(), ioException);
        }
    }
}
