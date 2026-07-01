package com.example.customer.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * MCP 工具客户端配置测试。
 * <p>
 * 验证默认 MCP Server JAR 路径在常见启动目录下都能稳定解析。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:25:00
 */
class McpToolClientConfigurationTest {

    @Test
    void shouldResolveDefaultServerJarFromAgentAppWorkingDirectory() {
        var resolved = McpToolClientConfiguration.resolveServerJar(
                "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar",
                Path.of("."));

        assertServerJarExists(resolved);
    }

    @Test
    void shouldResolveDefaultServerJarFromProjectRootWorkingDirectory() {
        var resolved = McpToolClientConfiguration.resolveServerJar(
                "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar",
                Path.of(".."));

        assertServerJarExists(resolved);
    }

    @Test
    void shouldResolveDefaultServerJarFromRepositoryRootWorkingDirectory() {
        var resolved = McpToolClientConfiguration.resolveServerJar(
                "../customer-mcp-server/target/customer-mcp-server-0.1.0-SNAPSHOT.jar",
                Path.of("../.."));

        assertServerJarExists(resolved);
    }

    @Test
    void shouldKeepExplicitAbsoluteServerJarPath() {
        var serverJar = Path.of(
                "customer-mcp-server",
                "target",
                "customer-mcp-server-0.1.0-SNAPSHOT.jar")
                .toAbsolutePath()
                .normalize();

        var resolved = McpToolClientConfiguration.resolveServerJar(serverJar.toString(), Path.of("."));

        assertThat(resolved).isEqualTo(serverJar);
    }

    private void assertServerJarExists(Path resolved) {
        assertThat(resolved).endsWith(Path.of(
                "customer-mcp-server",
                "target",
                "customer-mcp-server-0.1.0-SNAPSHOT.jar"));
        assertThat(Files.isRegularFile(resolved))
                .as("先执行 mvn -pl customer-mcp-server -am package 构建真实 MCP Server JAR")
                .isTrue();
    }
}
