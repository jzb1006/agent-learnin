package com.example.customer.agent.knowledge;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 知识条目写入请求。
 *
 * @param itemId 知识条目标识
 * @param category 知识分类
 * @param title 标题
 * @param content 正文
 * @param source 来源
 * @param version 版本
 * @param tags 标签
 * @author jiangzhibin
 * @since 2026-06-30 16:30:00
 */
public record KnowledgeItemRequest(
        @NotBlank(message = "itemId 不能为空") String itemId,
        @NotBlank(message = "category 不能为空") String category,
        @NotBlank(message = "title 不能为空") String title,
        @NotBlank(message = "content 不能为空") String content,
        String source,
        String version,
        List<String> tags) {
}
