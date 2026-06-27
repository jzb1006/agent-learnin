package com.example.customer.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;

/**
 * 验证本地 .env 能在 Spring Boot 启动早期进入 Environment。
 *
 * @author jiangzhibin
 * @since 2026-06-27 13:20:00
 */
class DotenvStartupTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldLoadDotenvFromConfiguredDirectoryDuringStartup() throws IOException {
        var envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                SPRING_AI_OPENAI_CHAT_MODEL=dotenv-startup-model
                DOTENV_STARTUP_SENTINEL=loaded-from-env-file
                """);

        var application = new SpringApplication(DotenvOnlyApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                Map.of(
                        "spring.config.name", "dotenv-startup-test",
                        "springdotenv.directory", tempDir.toString(),
                        "spring.ai.openai.chat.model", "${SPRING_AI_OPENAI_CHAT_MODEL:missing}"));

        try (var context = application.run()) {
            var environment = context.getEnvironment();

            assertThat(environment.getProperty("SPRING_AI_OPENAI_CHAT_MODEL")).isEqualTo("dotenv-startup-model");
            assertThat(environment.getProperty("spring.ai.openai.chat.model")).isEqualTo("dotenv-startup-model");
            assertThat(environment.getProperty("DOTENV_STARTUP_SENTINEL")).isEqualTo("loaded-from-env-file");
        }
    }

    @Test
    void shouldBindDotenvDirectoryFromApplicationYaml() throws IOException {
        var envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                SPRING_AI_OPENAI_CHAT_MODEL=dotenv-yaml-model
                """);

        var configFile = tempDir.resolve("application.yml");
        Files.writeString(configFile, """
                springdotenv:
                  directory: %s
                  filename: .env
                  ignore-if-missing: true
                spring:
                  ai:
                    openai:
                      chat:
                        model: ${SPRING_AI_OPENAI_CHAT_MODEL:missing}
                """.formatted(tempDir));

        var application = new SpringApplication(DotenvOnlyApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                Map.of(
                        "spring.config.location", configFile.toUri().toString()));

        try (var context = application.run()) {
            var environment = context.getEnvironment();

            assertThat(environment.getProperty("spring.ai.openai.chat.model")).isEqualTo("dotenv-yaml-model");
        }
    }

    @SpringBootConfiguration
    static class DotenvOnlyApplication {
    }
}
