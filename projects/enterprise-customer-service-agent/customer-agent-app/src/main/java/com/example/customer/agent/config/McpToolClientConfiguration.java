package com.example.customer.agent.config;

import com.example.customer.agent.mcp.McpToolClient;
import com.example.customer.agent.mcp.StdioMcpToolClient;
import com.example.customer.domain.support.DomainText;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具客户端配置。
 * <p>
 * 统一创建真实 stdio MCP client，避免对话编排层关心 transport 细节。
 *
 * @author jiangzhibin
 * @since 2026-07-01 09:56:00
 */
@Configuration
public class McpToolClientConfiguration {

    /**
     * 创建 MCP 工具客户端。
     *
     * @param properties 客服 Agent 配置
     * @return MCP 工具客户端
     */
    @Bean(destroyMethod = "close")
    public McpToolClient mcpToolClient(CustomerAgentProperties properties) {
        var mcpClient = properties.getMcpClient();
        var serverJar = resolveServerJar(mcpClient.getServerJar(), Path.of("."));
        return switch (mcpClient.getMode()) {
            case STDIO -> new StdioMcpToolClient(
                    mcpClient.getCommand(),
                    List.of("-jar", serverJar.toString()),
                    Map.of(),
                    Duration.ofSeconds(mcpClient.getRequestTimeoutSeconds()));
        };
    }

    static Path resolveServerJar(String configuredServerJar, Path workingDirectory) {
        var serverJar = Path.of(DomainText.requireNonBlank(configuredServerJar, "MCP stdio server jar"));
        if (serverJar.isAbsolute()) {
            return serverJar.normalize();
        }

        var baseDirectory = Objects.requireNonNull(workingDirectory, "working directory must not be null")
                .toAbsolutePath()
                .normalize();
        var directPath = baseDirectory.resolve(serverJar).normalize();
        if (Files.isRegularFile(directPath) || !isDefaultMcpServerJarPath(serverJar)) {
            return directPath;
        }
        return findMcpServerJar(baseDirectory, serverJar.getFileName()).orElse(directPath);
    }

    private static boolean isDefaultMcpServerJarPath(Path serverJar) {
        return serverJar.toString()
                .replace('\\', '/')
                .contains("customer-mcp-server/target/");
    }

    private static Optional<Path> findMcpServerJar(Path baseDirectory, Path fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        for (var current = baseDirectory; current != null; current = current.getParent()) {
            var projectRootCandidate = current.resolve("customer-mcp-server")
                    .resolve("target")
                    .resolve(fileName)
                    .normalize();
            if (Files.isRegularFile(projectRootCandidate)) {
                return Optional.of(projectRootCandidate);
            }

            var workspaceRootCandidate = current.resolve("projects")
                    .resolve("enterprise-customer-service-agent")
                    .resolve("customer-mcp-server")
                    .resolve("target")
                    .resolve(fileName)
                    .normalize();
            if (Files.isRegularFile(workspaceRootCandidate)) {
                return Optional.of(workspaceRootCandidate);
            }
        }
        return Optional.empty();
    }
}
