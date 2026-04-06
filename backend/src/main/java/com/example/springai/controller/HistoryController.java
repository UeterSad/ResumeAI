package com.example.springai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springai.model.ChatHistory;
import com.example.springai.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping("/getHistory")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        Page<ChatHistory> historyPage = historyService.queryHistoryPage(userId, pageNum, pageSize);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("records", historyPage.getRecords());
        responseBody.put("total", historyPage.getTotal());
        responseBody.put("current", historyPage.getCurrent());
        responseBody.put("size", historyPage.getSize());
        
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return getHistory(userId, pageNum, pageSize);
    }

    @GetMapping("/detail/{historyId}")
    public ResponseEntity<ChatHistory> getHistoryDetailById(@PathVariable String historyId) {
        ChatHistory history = historyService.findHistoryById(historyId);
        if (history != null) {
            return ResponseEntity.ok(history);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<Map<String, Object>> deleteHistory(
            @PathVariable String historyId,
            @RequestParam String userId) {
        boolean deleted = historyService.deleteHistoryById(historyId, userId);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", deleted);
        responseBody.put("message", deleted ? "删除成功" : "记录不存在或无权限删除");

        if (deleted) {
            return ResponseEntity.ok(responseBody);
        }
        return ResponseEntity.status(404).body(responseBody);
    }
}