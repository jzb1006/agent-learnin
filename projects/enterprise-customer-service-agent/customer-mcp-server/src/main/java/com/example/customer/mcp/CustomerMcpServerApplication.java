package com.example.customer.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客服订单 MCP Server 启动入口。
 * <p>
 * 通过 Spring AI MCP Server starter 暴露 Day 22 的只读客服工具。
 *
 * @author jiangzhibin
 * @since 2026-06-30 16:58:00
 */
@SpringBootApplication
public class CustomerMcpServerApplication {

    /**
     * 启动 MCP Server。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerMcpServerApplication.class, args);
    }
}
