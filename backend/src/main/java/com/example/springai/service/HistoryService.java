package com.example.springai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springai.model.ChatHistory;

public interface HistoryService {

    /**
     * 保存聊天历史。
     */
    void createHistory(ChatHistory history);

    /**
     * 更新聊天历史。
     */
    void updateHistory(ChatHistory history);

    /**
     * 按用户分页查询历史记录。
     */
    Page<ChatHistory> queryHistoryPage(String userId, int pageNum, int pageSize);

    /**
     * 根据历史记录 ID 查询详情。
     */
    ChatHistory findHistoryById(String historyId);

    /**
     * 删除历史记录（需校验用户归属）。
     */
    boolean deleteHistoryById(String historyId, String userId);
}
