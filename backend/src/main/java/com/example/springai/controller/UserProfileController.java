package com.example.springai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springai.mapper.UserProfileMapper;
import com.example.springai.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户个人信息控制器
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserProfileMapper userProfileMapper;

    private Map<String, Object> buildSuccessResponse(Object data) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", data);
        return responseBody;
    }

    private Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("error", errorMessage);
        return responseBody;
    }

    private UserProfile findProfileByUserId(String userId) {
        LambdaQueryWrapper<UserProfile> profileQuery = new LambdaQueryWrapper<>();
        profileQuery.eq(UserProfile::getUserId, userId);
        return userProfileMapper.selectOne(profileQuery);
    }

    /**
     * 将请求中的个人信息字段按需覆盖到实体。
     */
    private void applyProfileFields(UserProfile profileEntity, Map<String, Object> requestPayload) {
        if (requestPayload.containsKey("name")) {
            profileEntity.setName((String) requestPayload.get("name"));
        }
        if (requestPayload.containsKey("gender")) {
            profileEntity.setGender((String) requestPayload.get("gender"));
        }
        if (requestPayload.containsKey("birthDate") && requestPayload.get("birthDate") != null) {
            profileEntity.setBirthDate(LocalDate.parse((String) requestPayload.get("birthDate")));
        }
        if (requestPayload.containsKey("phone")) {
            profileEntity.setPhone((String) requestPayload.get("phone"));
        }
        if (requestPayload.containsKey("email")) {
            profileEntity.setEmail((String) requestPayload.get("email"));
        }
        if (requestPayload.containsKey("address")) {
            profileEntity.setAddress((String) requestPayload.get("address"));
        }
        if (requestPayload.containsKey("jobStatus")) {
            profileEntity.setJobStatus((String) requestPayload.get("jobStatus"));
        }
        if (requestPayload.containsKey("jobTitle")) {
            profileEntity.setJobTitle((String) requestPayload.get("jobTitle"));
        }
        if (requestPayload.containsKey("jobCity")) {
            profileEntity.setJobCity((String) requestPayload.get("jobCity"));
        }
        if (requestPayload.containsKey("salaryMin") && requestPayload.get("salaryMin") != null) {
            profileEntity.setSalaryMin(((Number) requestPayload.get("salaryMin")).intValue());
        }
        if (requestPayload.containsKey("salaryMax") && requestPayload.get("salaryMax") != null) {
            profileEntity.setSalaryMax(((Number) requestPayload.get("salaryMax")).intValue());
        }
        if (requestPayload.containsKey("school")) {
            profileEntity.setSchool((String) requestPayload.get("school"));
        }
        if (requestPayload.containsKey("major")) {
            profileEntity.setMajor((String) requestPayload.get("major"));
        }
        if (requestPayload.containsKey("degree")) {
            profileEntity.setDegree((String) requestPayload.get("degree"));
        }
        if (requestPayload.containsKey("graduationDate") && requestPayload.get("graduationDate") != null) {
            profileEntity.setGraduationDate(LocalDate.parse((String) requestPayload.get("graduationDate")));
        }
        if (requestPayload.containsKey("skills")) {
            profileEntity.setSkills((String) requestPayload.get("skills"));
        }
        if (requestPayload.containsKey("selfIntroduction")) {
            profileEntity.setSelfIntroduction((String) requestPayload.get("selfIntroduction"));
        }
    }

    private String buildSalaryExpectation(UserProfile profileEntity) {
        if (profileEntity.getSalaryMin() != null && profileEntity.getSalaryMax() != null) {
            return profileEntity.getSalaryMin() + "K-" + profileEntity.getSalaryMax() + "K";
        }
        if (profileEntity.getSalaryMin() != null) {
            return profileEntity.getSalaryMin() + "K以上";
        }
        if (profileEntity.getSalaryMax() != null) {
            return profileEntity.getSalaryMax() + "K以内";
        }
        return null;
    }

    private Map<String, Object> buildResumeDataFromProfile(UserProfile profileEntity) {
        Map<String, Object> resumeData = new HashMap<>();
        resumeData.put("name", profileEntity.getName());
        resumeData.put("phone", profileEntity.getPhone());
        resumeData.put("email", profileEntity.getEmail());
        resumeData.put("jobStatus", profileEntity.getJobStatus());
        resumeData.put("jobTitle", profileEntity.getJobTitle());

        String salaryExpectation = buildSalaryExpectation(profileEntity);
        if (salaryExpectation != null) {
            resumeData.put("salaryExpectation", salaryExpectation);
        }

        Map<String, Object> educationData = new HashMap<>();
        educationData.put("school", profileEntity.getSchool());
        educationData.put("major", profileEntity.getMajor());
        educationData.put("degree", profileEntity.getDegree());
        if (profileEntity.getGraduationDate() != null) {
            educationData.put("graduationDate", profileEntity.getGraduationDate().toString());
        }
        resumeData.put("education", educationData);

        Map<String, Object> professionData = new HashMap<>();
        professionData.put("skill", profileEntity.getSkills());
        resumeData.put("profession", professionData);
        return resumeData;
    }

    /**
     * 获取用户个人信息
     */
    @GetMapping("/{userId}")
    public Map<String, Object> getProfile(@PathVariable String userId) {
        try {
            UserProfile profileEntity = findProfileByUserId(userId);

            if (profileEntity == null) {
                Map<String, Object> responseBody = buildSuccessResponse(null);
                responseBody.put("message", "用户信息不存在，请先完善个人信息");
                return responseBody;
            }
            return buildSuccessResponse(profileEntity);
        } catch (Exception e) {
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * 保存或更新用户个人信息
     */
    @PostMapping("/save")
    public Map<String, Object> saveProfile(@RequestBody Map<String, Object> requestPayload) {
        try {
            String userId = (String) requestPayload.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return buildErrorResponse("用户ID不能为空");
            }

            UserProfile existingProfile = findProfileByUserId(userId);

            UserProfile profileEntity;
            if (existingProfile != null) {
                profileEntity = existingProfile;
            } else {
                profileEntity = new UserProfile();
                profileEntity.setUserId(userId);
                profileEntity.setCreateTime(LocalDateTime.now());
            }

            applyProfileFields(profileEntity, requestPayload);
            profileEntity.setUpdateTime(LocalDateTime.now());

            if (existingProfile != null) {
                userProfileMapper.updateById(profileEntity);
            } else {
                userProfileMapper.insert(profileEntity);
            }

            Map<String, Object> responseBody = buildSuccessResponse(profileEntity);
            responseBody.put("message", "保存成功");
            return responseBody;
        } catch (Exception e) {
            logger.error("保存用户画像失败", e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * 获取用于填充简历的数据
     */
    @GetMapping("/resume-data/{userId}")
    public Map<String, Object> getResumeData(@PathVariable String userId) {
        try {
            UserProfile profileEntity = findProfileByUserId(userId);

            if (profileEntity == null) {
                return buildErrorResponse("用户信息不存在");
            }

            return buildSuccessResponse(buildResumeDataFromProfile(profileEntity));
        } catch (Exception e) {
            return buildErrorResponse(e.getMessage());
        }
    }
}
