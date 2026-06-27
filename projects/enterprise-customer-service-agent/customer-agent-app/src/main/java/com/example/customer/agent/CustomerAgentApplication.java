package com.example.customer.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客服 Agent 应用入口。
 * <p>
 * 当前模块承载对话 API、Agent 编排和本地调试接口；Day 02 只建立可启动的应用骨架。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:29:00
 */
@SpringBootApplication
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
