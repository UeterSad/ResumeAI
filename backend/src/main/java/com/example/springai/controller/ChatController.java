package com.example.springai.controller;

import java.util.Map;
import java.util.HashMap;

import com.example.springai.model.UserAccount;
import com.example.springai.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.springai.model.UserResume;
import com.example.springai.service.ResumeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private UserService userService;
    @Autowired
    private ResumeService resumeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> asStringObjectMap(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> asStringObjectMapOrEmpty(Object value) {
        Map<String, Object> map = asStringObjectMap(value);
        return map != null ? map : new HashMap<>();
    }

    /**
     * 统一构建成功响应，保持前后端约定字段不变。
     */
    private Map<String, Object> buildSuccessResponse(String key, Object value) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put(key, value);
        return responseBody;
    }

    /**
     * 统一构建失败响应，减少重复代码。
     */
    private Map<String, Object> buildErrorResponse(Exception exception) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("error", exception.getMessage());
        return responseBody;
    }

    /**
     * 将个人信息补齐到 AI 解析出的简历结构中。
     */
    private void mergeProfileDataIntoResume(Map<String, Object> parsedResume, Map<String, Object> profileData) {
        if (profileData.get("name") != null) {
            parsedResume.put("name", profileData.get("name"));
        }
        if (profileData.get("phone") != null) {
            parsedResume.put("phone", profileData.get("phone"));
        }
        if (profileData.get("email") != null) {
            parsedResume.put("email", profileData.get("email"));
        }
        if (profileData.get("jobStatus") != null) {
            parsedResume.put("jobStatus", profileData.get("jobStatus"));
        }

        Map<String, Object> educationData = asStringObjectMap(parsedResume.get("education"));
        if (educationData != null) {
            if (profileData.get("school") != null && (educationData.get("school") == null || educationData.get("school").toString().isEmpty())) {
                educationData.put("school", profileData.get("school"));
            }
            if (profileData.get("major") != null && (educationData.get("major") == null || educationData.get("major").toString().isEmpty())) {
                educationData.put("major", profileData.get("major"));
            }
            if (profileData.get("degree") != null && (educationData.get("degree") == null || educationData.get("degree").toString().isEmpty())) {
                educationData.put("degree", profileData.get("degree"));
            }
        }

        if (profileData.get("skills") != null) {
            Map<String, Object> professionData = asStringObjectMap(parsedResume.get("profession"));
            if (professionData == null) {
                return;
            }
            String existingSkill = professionData.get("skill") != null ? professionData.get("skill").toString() : "";
            String profileSkills = profileData.get("skills").toString();
            if (!profileSkills.isEmpty() && !existingSkill.contains(profileSkills)) {
                professionData.put("skill", existingSkill.isEmpty() ? profileSkills : existingSkill + "、" + profileSkills);
            }
        }
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> requestPayload) {
        try {
            String userId = (String) requestPayload.get("userId");
            String username = (String) requestPayload.get("username");
            String resumeVersion = (String) requestPayload.getOrDefault("resumeVersion", "");
            Map<String, Object> formData = asStringObjectMapOrEmpty(requestPayload.get("formData"));
            Map<String, Object> profileData = asStringObjectMap(requestPayload.get("profileData"));

            String prompt = buildResumePrompt(resumeVersion, formData, profileData);
            String aiResponse = openAiChatModel.call(prompt);
            Map<String, Object> parsedResume = parseAiResponse(aiResponse);

            if (!parsedResume.containsKey("error")) {
                if (profileData != null) {
                    mergeProfileDataIntoResume(parsedResume, profileData);
                } else {
                    UserAccount user = userService.findUserById(userId);
                    if (user != null) {
                        parsedResume.put("name", user.getUsername());
                        parsedResume.put("phone", user.getPhone());
                        parsedResume.put("email", user.getEmail());
                    } else {
                        parsedResume.put("name", username != null ? username : "");
                    }
                }

                resumeService.createResume(userId, parsedResume);
            }

            return buildSuccessResponse("content", parsedResume);

        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/resume/latest")
    public Map<String, Object> fetchLatestResume(@RequestParam String userId) {
        try {
            UserResume resume = resumeService.findLatestResume(userId);
            if (resume == null) {
                throw new RuntimeException("No resume found");
            }

            return buildSuccessResponse("content", mapResumeEntity(resume));
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * 优化简历模块内容
     * @param request 包含 moduleType(模块类型) 和 content(当前内容)
     * @return 优化后的内容
     */
    @PostMapping("/resume/optimize")
    public Map<String, Object> optimizeResumeModule(@RequestBody Map<String, Object> request) {
        try {
            String moduleType = (String) request.get("moduleType");
            Object content = request.get("content");
            String targetPosition = (String) request.getOrDefault("targetPosition", "");
            
            String prompt = buildOptimizePrompt(moduleType, content, targetPosition);
            String aiResponse = openAiChatModel.call(prompt);

            return buildSuccessResponse("optimizedContent", parseOptimizedContent(moduleType, aiResponse));
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * 优化整份简历
     */
    @PostMapping("/resume/optimize-all")
    public Map<String, Object> optimizeFullResume(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> resumeData = asStringObjectMapOrEmpty(request.get("resumeData"));
            String targetPosition = (String) request.getOrDefault("targetPosition", "");
            
            String prompt = buildFullOptimizePrompt(resumeData, targetPosition);
            String aiResponse = openAiChatModel.call(prompt);
            Map<String, Object> optimizedData = parseAiResponse(aiResponse);

            return buildSuccessResponse("optimizedResume", optimizedData);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    @PostMapping("/resume/update")
    public Map<String, Object> updateUserResume(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Map<String, Object> resumeData = asStringObjectMapOrEmpty(request.get("resumeData"));
            
            UserResume resume = resumeService.updateResumeByUserId(userId, resumeData);

            return buildSuccessResponse("content", mapResumeEntity(resume));
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    private Map<String, Object> mapResumeEntity(UserResume resume) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("name", resume.getName());
            result.put("phone", resume.getPhone());
            result.put("email", resume.getEmail());
            result.put("avatar", resume.getAvatar());
            result.put("jobStatus", resume.getJobStatus());
            result.put("jobTitle", resume.getJobTitle());
            result.put("salaryExpectation", resume.getSalaryExpectation());
            
            // 解析JSON字符串为对象（兼容对象或数组）
            if (resume.getEducation() != null) {
                result.put("education", objectMapper.readValue(resume.getEducation(), Object.class));
            }
            if (resume.getProfession() != null) {
                result.put("profession", objectMapper.readValue(resume.getProfession(), Map.class));
            }
            if (resume.getWork() != null) {
                result.put("work", objectMapper.readValue(resume.getWork(), Object.class));
            }
            if (resume.getProject() != null) {
                result.put("project", objectMapper.readValue(resume.getProject(), Object.class));
            }
            if (resume.getAward() != null) {
                result.put("award", objectMapper.readValue(resume.getAward(), Map.class));
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert resume to map", e);
        }
    }

    private String buildResumePrompt(String resumeVersion, Map<String, Object> formData, Map<String, Object> profileData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成一份结构化的简历信息，严格按照以下JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"jobStatus\": \"在职/离职/应届生\",\n");
        prompt.append("  \"jobTitle\": \"期望职位\",\n");
        prompt.append("  \"salaryExpectation\": \"期望薪资\",\n");
        prompt.append("  \"education\": {\n");
        prompt.append("    \"school\": \"学校名称\",\n");
        prompt.append("    \"major\": \"专业名称\",\n");
        prompt.append("    \"degree\": \"学历\"\n");
        prompt.append("  },\n");
        prompt.append("  \"profession\": {\n");
        prompt.append("    \"skill\": \"技能描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"work\": {\n");
        prompt.append("    \"company\": \"公司名称\",\n");
        prompt.append("    \"department\": \"部门名称\",\n");
        prompt.append("    \"position\": \"职位名称\",\n");
        prompt.append("    \"details\": \"工作内容描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"project\": {\n");
        prompt.append("    \"name\": \"项目名称\",\n");
        prompt.append("    \"details\": \"项目描述\"\n");
        prompt.append("  },\n");
        prompt.append("  \"award\": {\n");
        prompt.append("    \"details\": \"获奖情况\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");

        // 添加个人信息作为参考
        if (profileData != null) {
            prompt.append("【用户个人信息（请参考并融入简历）】\n");
            if (profileData.get("name") != null) {
                prompt.append("姓名：").append(profileData.get("name")).append("\n");
            }
            if (profileData.get("jobStatus") != null) {
                prompt.append("求职状态：").append(profileData.get("jobStatus")).append("\n");
            }
            if (profileData.get("jobTitle") != null) {
                prompt.append("期望职位：").append(profileData.get("jobTitle")).append("\n");
            }
            if (profileData.get("jobCity") != null) {
                prompt.append("期望城市：").append(profileData.get("jobCity")).append("\n");
            }
            if (profileData.get("salaryMin") != null && profileData.get("salaryMax") != null) {
                prompt.append("期望薪资：").append(profileData.get("salaryMin")).append("K-").append(profileData.get("salaryMax")).append("K\n");
            }
            if (profileData.get("school") != null) {
                prompt.append("毕业院校：").append(profileData.get("school")).append("\n");
            }
            if (profileData.get("major") != null) {
                prompt.append("专业：").append(profileData.get("major")).append("\n");
            }
            if (profileData.get("degree") != null) {
                prompt.append("学历：").append(profileData.get("degree")).append("\n");
            }
            if (profileData.get("skills") != null) {
                prompt.append("技能标签：").append(profileData.get("skills")).append("\n");
            }
            if (profileData.get("selfIntroduction") != null) {
                prompt.append("自我介绍：").append(profileData.get("selfIntroduction")).append("\n");
            }
            prompt.append("\n");
        }

        if ("应届生版".equals(resumeVersion)) {
            prompt.append("基于以下信息生成应届生简历：\n");
            prompt.append("专业：").append(formData.get("major")).append("\n");
            prompt.append("期望职位：").append(formData.get("position")).append("\n");
            prompt.append("补充信息：").append(formData.get("extra")).append("\n");
        } else {
            prompt.append("基于以下信息生成标准简历：\n");
            prompt.append("工作经历：").append(formData.get("experience")).append("\n");
            prompt.append("期望职位：").append(formData.get("position")).append("\n");
            prompt.append("补充信息：").append(formData.get("extra")).append("\n");
        }

        return prompt.toString();
    }

    private Map<String, Object> parseAiResponse(String aiResponse) {
        try {
            // 清理和预处理 AI 响应
            String cleanedResponse = cleanAiResponse(aiResponse);

            // 使用 ObjectMapper 解析 JSON
            Map<String, Object> parsedData = objectMapper.readValue(cleanedResponse, new TypeReference<Map<String, Object>>() {});

            // 验证必要的字段
            validateResumeData(parsedData);

            return parsedData;
        } catch (Exception e) {
            logger.warn("解析 AI 响应失败", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "AI响应解析失败: " + e.getMessage());
            return fallback;
        }
    }

    private String cleanAiResponse(String aiResponse) {
        // 移除可能的前缀和后缀文本
        String cleaned = aiResponse.trim();

        // 查找第一个 { 和最后一个 } 的位置
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private void validateResumeData(Map<String, Object> data) {
        // 检查必要字段是否存在
        String[] requiredFields = {"jobStatus", "jobTitle", "salaryExpectation", "education", "profession", "work", "project", "award"};

        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                throw new IllegalArgumentException("缺少必要字段: " + field);
            }
        }

        // 验证嵌套对象
        validateNestedObject(data, "education", new String[]{"school", "major", "degree"});
        validateNestedObject(data, "profession", new String[]{"skill"});
        validateNestedObject(data, "work", new String[]{"company", "department", "position", "details"});
        validateNestedObject(data, "project", new String[]{"name", "details"});
        validateNestedObject(data, "award", new String[]{"details"});
    }

    private void validateNestedObject(Map<String, Object> data, String objectKey, String[] requiredFields) {
        Object obj = data.get(objectKey);
        if (!(obj instanceof Map)) {
            throw new IllegalArgumentException(objectKey + " 必须是一个对象");
        }

        Map<String, Object> nestedObj = asStringObjectMap(obj);
        if (nestedObj == null) {
            throw new IllegalArgumentException(objectKey + " 必须是一个对象");
        }
        for (String field : requiredFields) {
            if (!nestedObj.containsKey(field)) {
                throw new IllegalArgumentException(objectKey + " 缺少必要字段: " + field);
            }
        }
    }

    /**
     * 构建模块优化的 Prompt
     */
    private String buildOptimizePrompt(String moduleType, Object content, String targetPosition) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的简历优化顾问。请优化以下简历内容，使其更加专业、有吸引力。\n");
        prompt.append("【重要】返回的内容必须是纯文本格式，严禁使用任何Markdown标记（如**、##、-、*等），不要使用列表符号，直接用逗号或顿号分隔即可。\n\n");
        
        if (targetPosition != null && !targetPosition.isEmpty()) {
            prompt.append("目标职位：").append(targetPosition).append("\n\n");
        }
        
        // 将content转换为字符串
        String contentStr;
        try {
            if (content instanceof String) {
                contentStr = (String) content;
            } else {
                contentStr = objectMapper.writeValueAsString(content);
            }
        } catch (Exception e) {
            contentStr = content.toString();
        }

        switch (moduleType) {
            case "profession":
                prompt.append("请优化以下专业技能描述，使其更加专业和有条理。\n");
                prompt.append("当前内容：").append(contentStr).append("\n\n");
                prompt.append("请直接返回优化后的技能描述纯文本，不要使用Markdown格式，不要使用列表符号，技能之间用顿号或逗号分隔。");
                break;
            case "work":
                prompt.append("请优化以下工作经历描述，突出成就和贡献，使用STAR法则。\n");
                prompt.append("当前内容：").append(contentStr).append("\n\n");
                prompt.append("请返回JSON格式：{\"company\":\"公司\",\"department\":\"部门\",\"position\":\"职位\",\"details\":\"优化后的工作内容描述（纯文本，不要Markdown）\"}");
                break;
            case "project":
                prompt.append("请优化以下项目经历描述，突出技术栈、职责和成果。\n");
                prompt.append("当前内容：").append(contentStr).append("\n\n");
                prompt.append("请返回JSON格式：{\"name\":\"项目名称\",\"details\":\"优化后的项目描述（纯文本，不要Markdown）\"}");
                break;
            case "award":
                prompt.append("请优化以下荣誉奖项描述，使其更加规范和专业。\n");
                prompt.append("当前内容：").append(contentStr).append("\n\n");
                prompt.append("请直接返回优化后的奖项描述纯文本，不要使用Markdown格式。");
                break;
            default:
                prompt.append("请优化以下内容：\n").append(contentStr);
                prompt.append("\n请直接返回优化后的纯文本，不要使用Markdown格式。");
        }
        
        return prompt.toString();
    }

    /**
     * 构建整份简历优化的 Prompt
     */
    private String buildFullOptimizePrompt(Map<String, Object> resumeData, String targetPosition) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的简历优化顾问。请全面优化以下简历内容，使其更加专业、有吸引力，并针对目标职位进行优化。\n");
        prompt.append("【重要】所有文本内容必须是纯文本格式，严禁使用任何Markdown标记（如**、##、-、*等），不要使用列表符号，直接用逗号或顿号分隔即可。\n\n");
        
        if (targetPosition != null && !targetPosition.isEmpty()) {
            prompt.append("目标职位：").append(targetPosition).append("\n\n");
        }
        
        prompt.append("当前简历内容：\n");
        prompt.append(objectMapper.valueToTree(resumeData).toString()).append("\n\n");
        
        prompt.append("请严格按照以下JSON格式返回优化后的简历：\n");
        prompt.append("{\n");
        prompt.append("  \"jobStatus\": \"在职/离职/应届生\",\n");
        prompt.append("  \"jobTitle\": \"期望职位\",\n");
        prompt.append("  \"salaryExpectation\": \"期望薪资\",\n");
        prompt.append("  \"education\": {\"school\": \"学校\", \"major\": \"专业\", \"degree\": \"学历\"},\n");
        prompt.append("  \"profession\": {\"skill\": \"优化后的技能描述\"},\n");
        prompt.append("  \"work\": {\"company\": \"公司\", \"department\": \"部门\", \"position\": \"职位\", \"details\": \"优化后的工作描述\"},\n");
        prompt.append("  \"project\": {\"name\": \"项目名称\", \"details\": \"优化后的项目描述\"},\n");
        prompt.append("  \"award\": {\"details\": \"优化后的奖项描述\"}\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 解析优化后的内容
     */
    private Object parseOptimizedContent(String moduleType, String aiResponse) {
        try {
            String cleaned = aiResponse.trim();
            
            // 对于需要返回JSON的模块
            if ("work".equals(moduleType) || "project".equals(moduleType)) {
                int start = cleaned.indexOf("{");
                int end = cleaned.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    cleaned = cleaned.substring(start, end + 1);
                    return objectMapper.readValue(cleaned, Map.class);
                }
            }
            
            // 对于返回纯文本的模块
            return cleaned;
        } catch (Exception e) {
            return aiResponse.trim();
        }
    }
}
