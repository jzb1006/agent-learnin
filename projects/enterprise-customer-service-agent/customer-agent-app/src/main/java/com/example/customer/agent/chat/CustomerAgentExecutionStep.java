package com.example.customer.agent.chat;

import java.util.Objects;

/**
 * 客服 Agent 执行链路步骤。
 * <p>
 * 用于展示单次对话在 Java 编排层经历的关键阶段，方便调试台和后续评测定位失败点。
 *
 * @param name 步骤名称
 * @param detail 步骤详情
 * @author jiangzhibin
 * @since 2026-07-02 09:25:00
 */
public record CustomerAgentExecutionStep(
        String name,
        String detail) {

    /**
     * 创建执行步骤。
     */
    public CustomerAgentExecutionStep {
        Objects.requireNonNull(name, "execution step name must not be null");
        Objects.requireNonNull(detail, "execution step detail must not be null");
    }
}
