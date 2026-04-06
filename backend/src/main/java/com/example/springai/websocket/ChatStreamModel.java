package com.example.springai.websocket;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatStreamModel {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 根据输入内容生成流式回答。
     */
    public Flux<String> stream(String promptText) {
        return openAiChatModel.stream(promptText);
    }
}

