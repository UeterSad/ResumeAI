package com.example.springai.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import com.example.springai.model.ChatRequest;
import com.example.springai.model.ChatHistory;
import com.example.springai.service.HistoryService;
import com.example.springai.service.SendEmail;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final HistoryService historyService;
    private final ChatStreamModel openAiChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造注入历史服务与 AI 流式模型。
    public ChatWebSocketHandler(HistoryService historyService, ChatStreamModel openAiChatModel) {
        this.historyService = historyService;
        this.openAiChatModel = openAiChatModel;
    }

    // 保留旧构造器签名，避免手动 new 的配置类编译失败。
    public ChatWebSocketHandler(HistoryService historyService, ChatStreamModel openAiChatModel, SendEmail sendEmail) {
        this(historyService, openAiChatModel);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        String payload = message.getPayload();
        ChatRequest request = parsePayload(payload);
        String question = resolveQuestion(request);
        if (question == null || question.isBlank()) {
            logger.warn("收到空消息，忽略本次请求");
            return;
        }
        String userId = request.getUserId();
        String username = request.getUsername();
        String prompt = buildPromptWithHistory(request, question);

        StringBuilder responseBuilder = new StringBuilder();

        Flux<String> aiResponse = openAiChatModel.stream(prompt)
                .doOnNext(chunk -> {
                    if (!"[DONE]".equalsIgnoreCase(chunk)) {
                        responseBuilder.append(chunk);
                    }
                })
                .map(chunk -> "data:" + encodeStreamChunk(chunk) + "\n\n")
                .concatWith(Mono.just("data:[DONE]\n\n"));

        aiResponse.subscribe(response -> {
            try {
                session.sendMessage(new TextMessage(response));
            } catch (IOException e) {
                logger.error("WebSocket 发送消息失败", e);
            }
        }, error -> {
            logger.error("WebSocket AI 流式响应异常", error);
        }, () -> {
            // 在流式响应结束后按会话维度落库：同一会话更新同一条历史。
            String answer = responseBuilder.toString();
            Date now = new Date(System.currentTimeMillis());
            String conversationId = request.getConversationId();
            String recordId = buildConversationRecordId(userId, conversationId);

            ChatHistory existed = historyService.findHistoryById(recordId);
            if (existed == null) {
                ChatHistory history = new ChatHistory();
                history.setId(recordId);
                history.setQuestion(question);
                history.setResult(answer);
                history.setUserId(userId);
                history.setUsername(username);
                history.setTime(now);
                historyService.createHistory(history);
                return;
            }

            String mergedQuestion = appendTurn(existed.getQuestion(), "用户", question);
            String mergedResult = appendTurn(existed.getResult(), "AI", answer);

            existed.setQuestion(mergedQuestion);
            existed.setResult(mergedResult);
            existed.setUsername(username);
            existed.setTime(now);
            historyService.updateHistory(existed);



        });
    }


    private ChatRequest parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, ChatRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid message format: " + payload, e);
        }
    }

    private String encodeStreamChunk(String chunk) {
        try {
            return objectMapper.writeValueAsString(chunk == null ? "" : chunk);
        } catch (Exception e) {
            logger.warn("流式分片 JSON 编码失败，降级为原文发送", e);
            return chunk == null ? "" : chunk;
        }
    }

    private String resolveQuestion(ChatRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getText() != null && !request.getText().isBlank()) {
            return request.getText();
        }
        if (request.getMsg() != null && !request.getMsg().isBlank()) {
            return request.getMsg();
        }
        return null;
    }

    private String buildPromptWithHistory(ChatRequest request, String question) {
        if (request == null || request.getHistory() == null || request.getHistory().isEmpty()) {
            return question;
        }

        List<ChatRequest.ChatTurn> history = request.getHistory();
        int fromIndex = Math.max(0, history.size() - 10);
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是简优 AI 助手，请结合历史对话继续回答，不要丢失上下文。\n");

        for (int i = fromIndex; i < history.size(); i++) {
            ChatRequest.ChatTurn turn = history.get(i);
            if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) {
                continue;
            }
            String speaker = "assistant".equalsIgnoreCase(turn.getRole()) ? "AI" : "用户";
            promptBuilder.append(speaker).append("：").append(turn.getContent()).append("\n");
        }

        promptBuilder.append("用户：").append(question).append("\nAI：");
        return promptBuilder.toString();
    }

    private String appendTurn(String original, String role, String content) {
        if (content == null || content.isBlank()) {
            return original;
        }
        String line = role + "：" + content;
        if (original == null || original.isBlank()) {
            return line;
        }
        return original + "\n\n" + line;
    }

    private String buildConversationRecordId(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
        String source = (userId == null ? "guest" : userId) + ":" + conversationId;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
    }
}
