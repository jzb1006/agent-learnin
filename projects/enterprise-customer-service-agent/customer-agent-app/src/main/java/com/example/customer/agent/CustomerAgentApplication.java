package com.example.customer.agent;

import com.example.customer.agent.config.CustomerAgentProperties;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;

/**
 * 客服 Agent 应用入口。
 * <p>
 * 当前模块承载对话 API、Agent 编排和本地调试接口；Day 02 只建立可启动的应用骨架。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:29:00
 */
@SpringBootApplication(
        exclude = {
            PgVectorStoreAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class
        })
@EnableConfigurationProperties({CustomerAgentProperties.class, DataSourceProperties.class, PgVectorStoreProperties.class})
public class CustomerAgentApplication {

    private static final String MODULE_NAME = "customer-agent-app";

    /**
     * 启动客服 Agent 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerAgentApplication.class, args);
    }

    /**
     * 返回应用模块名称。
     *
     * @return 模块名称
     */
    public static String moduleName() {
        return MODULE_NAME;
    }
}
