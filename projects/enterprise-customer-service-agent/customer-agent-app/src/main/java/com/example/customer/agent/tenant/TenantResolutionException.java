package com.example.customer.agent.tenant;

/**
 * 租户解析异常。
 * <p>
 * 用于把缺失或非法的租户请求头转换为稳定 API 错误码。
 *
 * @author jiangzhibin
 * @since 2026-06-30 11:45:00
 */
public class TenantResolutionException extends RuntimeException {

    private final String errorCode;

    private TenantResolutionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建缺失租户异常。
     *
     * @param headerName 请求头名称
     * @return 租户解析异常
     */
    public static TenantResolutionException required(String headerName) {
        return new TenantResolutionException("TENANT_REQUIRED", "缺少必填请求头：" + headerName);
    }

    /**
     * 创建非法租户异常。
     *
     * @param headerName 请求头名称
     * @return 租户解析异常
     */
    public static TenantResolutionException invalid(String headerName) {
        return new TenantResolutionException("TENANT_INVALID", "请求头 " + headerName + " 只能包含字母、数字、点、下划线、冒号或连字符");
    }

    /**
     * 返回 API 错误码。
     *
     * @return API 错误码
     */
    public String errorCode() {
        return errorCode;
    }
}
