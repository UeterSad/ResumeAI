package com.example.springai.model;

import java.util.List;

import lombok.Data;

@Data
public class ChatRequest {
    private String type;
    private String text;
    private String msg;
    private String userId;
    private String username;
    private String conversationId;
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role;
        private String content;
    }
}
