# knowledge-base

FAQ、政策和产品知识目录。

Day 16 开始填充租户级知识库样例。默认租户知识库结构如下：

```text
knowledge-base/
  default/
    faq/
    policies/
    products/
```

## 元数据约定

每份知识使用 Markdown 文件保存，并在文件开头提供 YAML front matter：

```yaml
---
title: "课程适合哪些学员"
source: "week10/work_v3/datas/data.txt#Q1-Q6"
tenant: "default"
version: "2026-06-29"
category: "FAQ"
tags:
  - "course-fit"
---
```

必填字段：

| 字段 | 说明 |
| --- | --- |
| `source` | 原始来源或业务口径出处，后续用于 RAG source 引用 |
| `tenant` | 租户标识，默认样例为 `default` |
| `version` | 知识版本，建议使用发布日期或业务版本号 |
| `category` | 知识分类，取值与领域模型 `KnowledgeCategory` 对齐：`FAQ`、`POLICY`、`PRODUCT` |

## 当前样例

| 目录 | 内容 |
| --- | --- |
| `default/faq/` | 学员适配、私有化模型部署等常见问题 |
| `default/policies/` | 退款、课程交付、资料下载、发票等政策 |
| `default/products/` | 企业级 AI Agent 实战课程、轻量化 Hybrid Agent 方案 |
