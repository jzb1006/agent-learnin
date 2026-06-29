---
title: "轻量化 Hybrid Agent 方案"
source: "week10/work_v3/datas/data.txt#Q25"
tenant: "default"
version: "2026-06-29"
category: "PRODUCT"
tags:
  - "hybrid-agent"
  - "small-business"
  - "cost-control"
---

# 轻量化 Hybrid Agent 方案

## 产品定位

轻量化 Hybrid Agent 方案面向预算、算力或运维团队规模有限的中小企业。方案优先使用规则引擎处理确定性流程，再由小模型或托管模型处理自然语言理解和回复生成。

## 适用场景

- FAQ 和政策问答相对稳定
- 订单、售后、工单等业务查询接口明确
- 不希望一开始部署复杂多 Agent 工作流
- 需要控制模型调用成本和基础设施复杂度

## 边界

该方案适合作为企业 Agent 落地的低风险起点，不替代高风险审批、支付退款、生产数据库写入等需要人工确认的流程。
