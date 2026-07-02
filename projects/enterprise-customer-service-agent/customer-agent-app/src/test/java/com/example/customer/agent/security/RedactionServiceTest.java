package com.example.customer.agent.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedactionServiceTest {

    @Test
    void shouldRedactPasswordsIdentityCardsBankCardsAndTokens() {
        var redactionService = new RedactionService();
        var rawText = "密码是 123456，身份证 110101199003077777，银行卡 6222020202020202020，"
                + "Authorization: Bearer sk-test-token-abcdef，access_token=secret-token-value";

        var redacted = redactionService.redact(rawText);

        assertThat(redacted)
                .contains("密码是 [REDACTED_PASSWORD]")
                .contains("身份证 [REDACTED_ID_CARD]")
                .contains("银行卡 [REDACTED_BANK_CARD]")
                .contains("Authorization: Bearer [REDACTED_TOKEN]")
                .contains("access_token=[REDACTED_TOKEN]");
        assertThat(redacted)
                .doesNotContain("123456")
                .doesNotContain("110101199003077777")
                .doesNotContain("6222020202020202020")
                .doesNotContain("sk-test-token-abcdef")
                .doesNotContain("secret-token-value");
    }

    @Test
    void shouldKeepNullAndBlankInputsStable() {
        var redactionService = new RedactionService();

        assertThat(redactionService.redact(null)).isEmpty();
        assertThat(redactionService.redact("   ")).isEqualTo("   ");
    }
}
