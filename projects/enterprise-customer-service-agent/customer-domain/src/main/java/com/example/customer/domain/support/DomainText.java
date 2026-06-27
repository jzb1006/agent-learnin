package com.example.customer.domain.support;

/**
 * 领域文本校验工具。
 * <p>
 * 统一处理领域模型中的必填文本校验，避免各聚合对象重复编写空值判断。
 *
 * @author jiangzhibin
 * @since 2026-06-27 08:45:00
 */
public final class DomainText {

    private DomainText() {
    }

    /**
     * 校验文本不能为空，并返回去除首尾空白后的值。
     *
     * @param value 原始文本
     * @param fieldName 字段名
     * @return 去除首尾空白后的文本
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
