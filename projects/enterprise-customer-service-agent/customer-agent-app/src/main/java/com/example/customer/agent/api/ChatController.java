package com.example.customer.agent.api;

import com.example.customer.agent.chat.ChatRequest;
import com.example.customer.agent.chat.ChatResponse;
import com.example.customer.agent.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服对话 API。
 * <p>
 * 当前只提供基础结构化响应，后续再接入 Spring AI ChatClient 和工具调用链。
 *
 * @author jiangzhibin
 * @since 2026-06-27 09:35:00
 */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 处理客服对话请求。
     *
     * @param request 对话请求
     * @return 结构化对话响应
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.reply(request);
    }
}
