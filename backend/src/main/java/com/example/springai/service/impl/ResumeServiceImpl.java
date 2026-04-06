package com.example.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springai.mapper.UserResumeMapper;
import com.example.springai.model.UserResume;
import com.example.springai.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private UserResumeMapper resumeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public UserResume createResume(String userId, Map<String, Object> resumeData) {
        UserResume resumeEntity = new UserResume();
        resumeEntity.setUserId(userId);
        applyResumeFields(resumeEntity, resumeData);
        resumeEntity.setUpdateTime(new Date());

        resumeMapper.insert(resumeEntity);
        return resumeEntity;
    }

    @Override
    public UserResume findLatestResume(String userId) {
        LambdaQueryWrapper<UserResume> latestResumeQuery = new LambdaQueryWrapper<>();
        latestResumeQuery.eq(UserResume::getUserId, userId)
                .orderByDesc(UserResume::getUpdateTime)
                .last("LIMIT 1");
        return resumeMapper.selectOne(latestResumeQuery);
    }


    @Override
    @Transactional
    public UserResume updateResumeByUserId(String userId, Map<String, Object> resumeData) {
        UserResume resumeEntity = findLatestResume(userId);
        if (resumeEntity == null) {
            return createResume(userId, resumeData);
        }

        applyResumeFields(resumeEntity, resumeData);
        resumeEntity.setUpdateTime(new Date());

        resumeMapper.updateById(resumeEntity);
        return resumeEntity;
    }

    /**
     * 将请求字段映射到简历实体，复杂对象按 JSON 字符串存储。
     */
    private void applyResumeFields(UserResume resumeEntity, Map<String, Object> data) {
        try {
            if (data.get("name") != null) resumeEntity.setName((String) data.get("name"));
            if (data.get("phone") != null) resumeEntity.setPhone((String) data.get("phone"));
            if (data.get("email") != null) resumeEntity.setEmail((String) data.get("email"));
            if (data.get("avatar") != null) resumeEntity.setAvatar((String) data.get("avatar"));
            if (data.get("jobStatus") != null) resumeEntity.setJobStatus((String) data.get("jobStatus"));
            if (data.get("jobTitle") != null) resumeEntity.setJobTitle((String) data.get("jobTitle"));
            if (data.get("salaryExpectation") != null) resumeEntity.setSalaryExpectation((String) data.get("salaryExpectation"));

            if (data.get("education") != null) {
                resumeEntity.setEducation(objectMapper.writeValueAsString(data.get("education")));
            }
            if (data.get("profession") != null) {
                resumeEntity.setProfession(objectMapper.writeValueAsString(data.get("profession")));
            }
            if (data.get("work") != null) {
                resumeEntity.setWork(objectMapper.writeValueAsString(data.get("work")));
            }
            if (data.get("project") != null) {
                resumeEntity.setProject(objectMapper.writeValueAsString(data.get("project")));
            }
            if (data.get("award") != null) {
                resumeEntity.setAward(objectMapper.writeValueAsString(data.get("award")));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update resume fields", e);
        }
    }
}
