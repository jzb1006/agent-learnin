package com.example.customer.domain;

/**
 * 客服订单领域模块锚点。
 * <p>
 * Day 02 仅用于验证多模块工程识别和测试链路，具体领域模型从 Day 03 开始补齐。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:29:00
 */
public final class DomainModule {

    private static final String MODULE_NAME = "customer-domain";

    private DomainModule() {
    }

    /**
     * 返回领域模块名称。
     *
     * @return 模块名称
     */
    public static String name() {
        return MODULE_NAME;
    }
}
