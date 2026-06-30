package com.example.customer.agent.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 租户解析器。
 * <p>
 * 从 HTTP 请求头解析租户标识，并在入口处完成格式校验。
 *
 * @author jiangzhibin
 * @since 2026-06-30 11:45:00
 */
@Component
public class TenantResolver {

    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final int MAX_TENANT_ID_LENGTH = 128;
    private static final Pattern SAFE_TENANT_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    /**
     * 解析必填租户标识。
     *
     * @param request HTTP 请求
     * @return 租户标识
     */
    public String resolveRequiredTenantId(HttpServletRequest request) {
        var tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            throw TenantResolutionException.required(TENANT_ID_HEADER);
        }
        var normalizedTenantId = tenantId.trim();
        if (normalizedTenantId.length() > MAX_TENANT_ID_LENGTH
                || !SAFE_TENANT_ID.matcher(normalizedTenantId).matches()) {
            throw TenantResolutionException.invalid(TENANT_ID_HEADER);
        }
        return normalizedTenantId;
    }
}
