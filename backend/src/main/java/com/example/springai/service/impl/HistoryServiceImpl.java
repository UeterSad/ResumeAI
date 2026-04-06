package com.example.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springai.mapper.ChatHistoryMapper;
import com.example.springai.model.ChatHistory;
import com.example.springai.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private ChatHistoryMapper historyMapper;

    @Override
    public Page<ChatHistory> queryHistoryPage(String userId, int pageNum, int pageSize) {
        Page<ChatHistory> historyPage = new Page<>(pageNum, pageSize);
        QueryWrapper<ChatHistory> historyQuery = new QueryWrapper<>();
        historyQuery.eq("user_id", userId)
               .orderByDesc("time");
        return historyMapper.selectPage(historyPage, historyQuery);
    }

    @Override
    public ChatHistory findHistoryById(String historyId) {
        return historyMapper.selectById(historyId);
    }

    @Override
    public void createHistory(ChatHistory history) {
        historyMapper.insert(history);
    }

    @Override
    public void updateHistory(ChatHistory history) {
        historyMapper.updateById(history);
    }

    @Override
    public boolean deleteHistoryById(String historyId, String userId) {
        ChatHistory history = historyMapper.selectById(historyId);
        if (history == null) {
            return false;
        }
        if (!Objects.equals(history.getUserId(), userId)) {
            return false;
        }
        return historyMapper.deleteById(historyId) > 0;
    }

}
