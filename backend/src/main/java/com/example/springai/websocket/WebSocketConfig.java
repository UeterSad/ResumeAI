package com.example.springai.websocket;

import com.example.springai.service.SendEmail;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.springai.service.HistoryService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatStreamModel openAiChatModel;
    private final HistoryService historyService;
    private final SendEmail sendEmail;

    @Autowired
    public WebSocketConfig(ChatStreamModel openAiChatModel, HistoryService historyService,
                            SendEmail sendEmail) {
        this.openAiChatModel = openAiChatModel;
        this.historyService = historyService;
        this.sendEmail = sendEmail;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(historyService, openAiChatModel, sendEmail), "/chat/socket")
            .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
