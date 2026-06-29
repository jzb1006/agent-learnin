---
title: "是否支持私有化模型部署"
source: "week10/work_v3/datas/data.txt#Q21-Q25"
tenant: "default"
version: "2026-06-29"
category: "FAQ"
tags:
  - "private-deployment"
  - "llm"
  - "hybrid-agent"
---

# 是否支持私有化模型部署

## 问题

课程是否支持本地或私有化部署模型？如果算力有限怎么办？

## 答案

课程支持私有化部署模型，会覆盖 Ollama、vLLM、TGI 等本地推理服务，并说明如何把这些服务接入 Agent 框架。

如果 GPU 资源有限，基础实验可先使用单卡消费级显卡或托管模型服务完成；高阶训练建议使用云平台并配合成本估算。对于中小企业场景，可以采用规则引擎和小模型协同的 Hybrid Agent 方案，降低算力依赖。
