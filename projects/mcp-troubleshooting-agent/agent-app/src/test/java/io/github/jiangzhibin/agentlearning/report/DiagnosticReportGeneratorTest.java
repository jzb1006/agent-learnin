package io.github.jiangzhibin.agentlearning.report;

import io.github.jiangzhibin.agentlearning.llm.ChatModelClient;
import io.github.jiangzhibin.agentlearning.llm.ChatModelResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticReportGeneratorTest {

    @Test
    void generateShouldAskModelForJsonReportWithSchemaConstraints() {
        var client = new FakeChatModelClient(List.of(new ChatModelResponse("""
            {
              "summary": "登录接口 500，优先检查数据库连接池。",
              "evidence": ["日志出现连接池超时"],
              "nextActions": ["只读查看错误日志"],
              "riskLevel": "MEDIUM"
            }
            """, "fake-model")));
        var generator = new DiagnosticReportGenerator(client, new DiagnosticReportParser(), 1);

        var report = generator.generate("登录接口 500，请给出只读排查报告");

        assertEquals(RiskLevel.MEDIUM, report.riskLevel());
        assertEquals(1, client.prompts().size());
        assertTrue(client.prompts().getFirst().contains("summary"));
        assertTrue(client.prompts().getFirst().contains("evidence"));
        assertTrue(client.prompts().getFirst().contains("nextActions"));
        assertTrue(client.prompts().getFirst().contains("riskLevel"));
        assertTrue(client.prompts().getFirst().contains("只输出一个 JSON 对象"));
    }

    @Test
    void generateShouldRetryOnceWhenModelReturnsInvalidJson() {
        var client = new FakeChatModelClient(List.of(
            new ChatModelResponse("这不是 JSON", "fake-model"),
            new ChatModelResponse("""
                {
                  "summary": "登录接口 500，优先检查错误日志。",
                  "evidence": ["接口返回 HTTP 500"],
                  "nextActions": ["只读查看应用日志"],
                  "riskLevel": "LOW"
                }
                """, "fake-model")
        ));
        var generator = new DiagnosticReportGenerator(client, new DiagnosticReportParser(), 2);

        var report = generator.generate("登录接口 500，请给出只读排查报告");

        assertEquals(RiskLevel.LOW, report.riskLevel());
        assertEquals(2, client.prompts().size());
        assertTrue(client.prompts().getLast().contains("上一次输出解析失败"));
    }

    private static class FakeChatModelClient implements ChatModelClient {

        private final List<ChatModelResponse> responses;
        private final AtomicInteger index = new AtomicInteger();
        private final List<String> prompts = new ArrayList<>();

        private FakeChatModelClient(List<ChatModelResponse> responses) {
            this.responses = responses;
        }

        @Override
        public ChatModelResponse complete(String userQuestion) {
            prompts.add(userQuestion);
            return responses.get(index.getAndIncrement());
        }

        private List<String> prompts() {
            return prompts;
        }
    }
}
