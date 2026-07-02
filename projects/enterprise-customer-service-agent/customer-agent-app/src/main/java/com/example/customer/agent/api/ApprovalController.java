package com.example.customer.agent.api;

import com.example.customer.agent.approval.ApprovalCreateRequest;
import com.example.customer.agent.approval.ApprovalResponse;
import com.example.customer.agent.approval.ApprovalService;
import com.example.customer.agent.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批调试 API。
 * <p>
 * 用于本地调试台模拟高风险动作审批请求创建，不执行真实退款、取消或改签。
 *
 * @author jiangzhibin
 * @since 2026-07-01 17:40:00
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 创建待审批请求。
     *
     * @param request 创建请求
     * @return 审批调试响应
     */
    @PostMapping
    public ApprovalResponse create(@Valid @RequestBody ApprovalCreateRequest request) {
        var tenantId = TenantContext.requireCurrentTenantId();
        log.info("approval_api_create tenantId={} orderId={} action={}", tenantId, request.orderId(), request.action());
        return approvalService.createPending(tenantId, request);
    }
}
