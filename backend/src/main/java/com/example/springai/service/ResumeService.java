package com.example.springai.service;

import com.example.springai.model.UserResume;
import java.util.Map;

public interface ResumeService {
    /**
     * 创建简历记录。
     */
    UserResume createResume(String userId, Map<String, Object> resumeData);

    /**
     * 查询用户最新简历。
     */
    UserResume findLatestResume(String userId);

    /**
     * 按用户更新简历，若不存在则创建。
     */
    UserResume updateResumeByUserId(String userId, Map<String, Object> resumeData);
} 