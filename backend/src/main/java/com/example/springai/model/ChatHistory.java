package com.example.springai.model;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("chat_history")
public class ChatHistory {
    private String id;
    private String question;
    private String result;
    private String username;
    private String userId;
    private Date time;
}
