package io.github.jiangzhibin.agentlearning.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticReportParserTest {

    private final DiagnosticReportParser parser = new DiagnosticReportParser();

    @Test
    void parseShouldConvertValidJsonToDiagnosticReport() {
        var json = """
            {
              "summary": "登录接口 500，优先怀疑数据库连接池耗尽。",
              "evidence": [
                "日志出现 HikariPool timeout",
                "最近一次发布修改了数据源配置"
              ],
              "nextActions": [
                "只读查看应用错误日志",
                "只读检查最近 Git 提交"
              ],
              "riskLevel": "MEDIUM"
            }
            """;

        var report = parser.parse(json);

        assertEquals("登录接口 500，优先怀疑数据库连接池耗尽。", report.summary());
        assertEquals(RiskLevel.MEDIUM, report.riskLevel());
        assertEquals(2, report.evidence().size());
        assertTrue(report.evidence().contains("日志出现 HikariPool timeout"));
        assertEquals("只读查看应用错误日志", report.nextActions().getFirst());
    }

    @Test
    void parseShouldRejectMissingRequiredField() {
        var json = """
            {
              "summary": "登录接口 500。",
              "evidence": ["日志出现异常"],
              "riskLevel": "LOW"
            }
            """;

        var exception = assertThrows(DiagnosticReportParseException.class, () -> parser.parse(json));

        assertTrue(exception.getMessage().contains("nextActions"));
    }

    @Test
    void parseShouldRejectUnknownRiskLevel() {
        var json = """
            {
              "summary": "登录接口 500。",
              "evidence": ["日志出现异常"],
              "nextActions": ["只读查看错误日志"],
              "riskLevel": "CRITICAL"
            }
            """;

        var exception = assertThrows(DiagnosticReportParseException.class, () -> parser.parse(json));

        assertTrue(exception.getMessage().contains("riskLevel"));
    }
}
