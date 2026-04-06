package com.example.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springai.mapper.JobPositionMapper;
import com.example.springai.model.JobPosition;
import com.example.springai.model.UserResume;
import com.example.springai.service.RecommendService;
import com.example.springai.service.ResumeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendServiceImpl.class);

    @Autowired
    private JobPositionMapper jobPositionMapper;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseEntity<?> recommendJobs(Map<String, Object> requestPayload) {
        try {
            String userId = (String) requestPayload.get("userId");

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "用户ID不能为空"));
            }

            UserResume resume = resumeService.findLatestResume(userId);
            if (resume == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "未找到用户简历信息，请先完善简历"));
            }

            Map<String, Object> recommendationContext = extractRecommendationContextFromResume(resume);
            String skills = (String) recommendationContext.get("skills");

            if (skills == null || skills.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "简历中未找到技能信息，请完善简历中的专业技能"));
            }

            String educationLevel = (String) recommendationContext.get("educationLevel");
            Integer experience = (Integer) recommendationContext.get("experience");
            String location = (String) recommendationContext.get("location");

                    List<String> parsedSkills = Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
                    final List<String> skillsList = expandSkillKeywords(parsedSkills);

            LambdaQueryWrapper<JobPosition> queryWrapper = new LambdaQueryWrapper<>();

            logger.debug("提取到的技能列表: {}", skillsList);

            if (!skillsList.isEmpty()) {
                queryWrapper.and(wrapper -> {
                    for (String skill : skillsList) {
                        String likeSkill = "%" + skill + "%";
                        logger.debug("添加技能匹配条件: {}", likeSkill);
                        wrapper.or().like(JobPosition::getRequiredSkills, likeSkill);
                    }
                });
            } else {
                logger.warn("技能列表为空，将回退到全量职位候选");
            }

            if (educationLevel != null && !educationLevel.trim().isEmpty()) {
                String likeEducation = "%" + educationLevel + "%";
                logger.debug("添加教育匹配条件: {}", likeEducation);
                queryWrapper.like(JobPosition::getEducationRequirement, likeEducation);
            }

            if (location != null && !location.trim().isEmpty()) {
                String likeLocation = "%" + location + "%";
                logger.debug("添加地点匹配条件: {}", likeLocation);
                queryWrapper.like(JobPosition::getLocation, likeLocation);
            }

            Long totalCount = jobPositionMapper.selectCount(null);
            logger.debug("数据库中职位总数: {}", totalCount);

            if (totalCount == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("jobs", Collections.emptyList());
                response.put("no_match_guidance", buildNoMatchGuidance(resume, skillsList));
                response.put("message", "数据库中没有职位数据，请先添加职位信息");
                return ResponseEntity.ok(response);
            }

            List<JobPosition> jobPositions = jobPositionMapper.selectList(queryWrapper);
            logger.debug("首次查询职位数量: {}", (jobPositions != null ? jobPositions.size() : 0));

            if (jobPositions == null || jobPositions.isEmpty()) {
                logger.info("未命中职位，开始放宽查询条件");

                queryWrapper = new LambdaQueryWrapper<>();
                if (!skillsList.isEmpty()) {
                    String firstSkill = "%" + skillsList.get(0) + "%";
                    logger.debug("使用首要技能查询: {}", firstSkill);
                    queryWrapper.like(JobPosition::getRequiredSkills, firstSkill);
                }

                jobPositions = jobPositionMapper.selectList(queryWrapper);
                logger.debug("放宽条件后职位数量: {}", (jobPositions != null ? jobPositions.size() : 0));
            }

            if (jobPositions == null) {
                jobPositions = new ArrayList<>();
            }

            List<Map<String, Object>> recommendedJobs = new ArrayList<>();
            for (JobPosition job : jobPositions) {
                int matchScore = calculateJobMatchScore(job.getRequiredSkills(), skillsList,
                                                   educationLevel, experience, location, job.getLocation());

                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", job.getId());
                jobMap.put("title", job.getTitle());
                jobMap.put("company_name", job.getCompanyName());
                jobMap.put("salary_range", job.getSalaryRange());
                jobMap.put("location", job.getLocation());
                jobMap.put("education_requirements", job.getEducationRequirement());
                jobMap.put("experience_requirements", job.getExperienceRequirement());
                jobMap.put("matchScore", matchScore);

                recommendedJobs.add(jobMap);
            }

            recommendedJobs.sort((a, b) -> {
                Integer scoreA = (Integer) a.get("matchScore");
                Integer scoreB = (Integer) b.get("matchScore");
                return scoreB.compareTo(scoreA);
            });

            if (recommendedJobs.size() > 10) {
                recommendedJobs = recommendedJobs.subList(0, 10);
            }

            int topJobCount = Math.min(3, recommendedJobs.size());
            logger.debug("将为前 {} 个职位生成个性化建议", topJobCount);

            for (int i = 0; i < topJobCount; i++) {
                Map<String, Object> jobMap = recommendedJobs.get(i);
                Object jobIdObj = jobMap.get("id");
                Long jobId = null;

                if (jobIdObj instanceof Integer) {
                    jobId = ((Integer) jobIdObj).longValue();
                } else if (jobIdObj instanceof Long) {
                    jobId = (Long) jobIdObj;
                } else if (jobIdObj instanceof String) {
                    try {
                        jobId = Long.parseLong((String) jobIdObj);
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析职位ID: {}", jobIdObj);
                    }
                }

                if (jobId == null) {
                    logger.warn("跳过职位，ID转换失败: {}", jobIdObj);
                    continue;
                }

                final Long finalJobId = jobId;
                JobPosition job = jobPositions.stream()
                                           .filter(j -> j.getId().equals(finalJobId))
                                           .findFirst()
                                           .orElse(null);

                if (job != null) {
                    String recommendationReason = buildPersonalizedRecommendationReason(resume, job, skillsList);
                    jobMap.put("recommendation_reason", recommendationReason);
                    logger.debug("已生成推荐理由: {}", job.getTitle());

                    String resumeImprovement = buildResumeImprovementSuggestion(resume, job);
                    jobMap.put("resume_improvement", resumeImprovement);
                    logger.debug("已生成简历优化建议: {}", job.getTitle());
                } else {
                    logger.warn("未找到ID为 {} 的职位对象", jobId);
                }
            }

            if (!recommendedJobs.isEmpty()) {
                Map<String, Object> careerAdvice = buildCareerAdvice(resume, recommendedJobs);
                Map<String, Object> response = new HashMap<>();
                response.put("jobs", recommendedJobs);
                response.put("career_advice", careerAdvice);
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("jobs", Collections.emptyList());
            response.put("no_match_guidance", buildNoMatchGuidance(resume, skillsList));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("职位推荐接口处理失败", e);
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    private String buildPersonalizedRecommendationReason(UserResume resume, JobPosition job, List<String> skills) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("我需要你作为一名职业顾问，为求职者推荐一个职位并给出个性化的推荐理由。\n\n");

            prompt.append("求职者信息：\n");
            prompt.append("- 教育背景：").append(resume.getEducation()).append("\n");
            prompt.append("- 专业技能：").append(String.join(", ", skills)).append("\n");
            prompt.append("- 工作经验：").append(resume.getWork()).append("\n");
            prompt.append("- 期望职位：").append(resume.getJobTitle()).append("\n\n");

            prompt.append("职位信息：\n");
            prompt.append("- 职位名称：").append(job.getTitle()).append("\n");
            prompt.append("- 公司名称：").append(job.getCompanyName()).append("\n");
            prompt.append("- 所需技能：").append(job.getRequiredSkills()).append("\n");
            prompt.append("- 职位描述：").append(job.getDescription()).append("\n\n");

            prompt.append("请针对这位求职者的背景和技能，生成一段个性化的职位推荐理由，解释为什么这个职位适合他/她。");
            prompt.append("理由应该分析求职者的技能如何与职位要求匹配，以及这个职位如何有助于他/她的职业发展。");
            prompt.append("回答应简洁有力，不超过100字。\n");

            String aiResponse = openAiChatModel.call(prompt.toString());
            return aiResponse.trim().replaceAll("^\"|\"$", "");

        } catch (Exception e) {
            logger.error("生成个性化推荐理由失败", e);
            return "这个职位与您的技能和经验非常匹配，值得考虑。";
        }
    }

    private String buildResumeImprovementSuggestion(UserResume resume, JobPosition job) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为一名专业的简历顾问，我需要你分析一位求职者的简历与特定职位的匹配度，并提供简历优化建议。\n\n");

            prompt.append("求职者简历信息：\n");
            prompt.append("- 教育背景：").append(resume.getEducation()).append("\n");
            prompt.append("- 专业技能：").append(resume.getProfession()).append("\n");
            prompt.append("- 工作经验：").append(resume.getWork()).append("\n");
            prompt.append("- 项目经历：").append(resume.getProject()).append("\n\n");

            prompt.append("目标职位信息：\n");
            prompt.append("- 职位名称：").append(job.getTitle()).append("\n");
            prompt.append("- 所需技能：").append(job.getRequiredSkills()).append("\n");
            prompt.append("- 职位要求：").append(job.getDescription()).append("\n\n");

            prompt.append("请针对这位求职者的简历与目标职位的匹配情况，提供1-2条具体的简历优化建议，");
            prompt.append("帮助求职者提高简历对这个特定职位的竞争力。建议应该简洁明了，不超过50字。\n");

            String aiResponse = openAiChatModel.call(prompt.toString());
            return aiResponse.trim().replaceAll("^\"|\"$", "");

        } catch (Exception e) {
            logger.error("生成简历优化建议失败", e);
            return "建议突出展示与该职位最相关的技能和经验，并量化您的成就。";
        }
    }

    private Map<String, Object> buildCareerAdvice(UserResume resume, List<Map<String, Object>> recommendedJobs) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为一名职业规划专家，我需要你为一位求职者提供职业发展建议。\n\n");

            prompt.append("求职者背景：\n");
            if (resume.getEducation() != null) prompt.append("- 教育背景：").append(resume.getEducation()).append("\n");
            if (resume.getProfession() != null) prompt.append("- 专业技能：").append(resume.getProfession()).append("\n");
            if (resume.getWork() != null) prompt.append("- 工作经验：").append(resume.getWork()).append("\n");
            if (resume.getJobTitle() != null) prompt.append("- 期望职位：").append(resume.getJobTitle()).append("\n\n");

            prompt.append("推荐职位：\n");
            for (int i = 0; i < Math.min(3, recommendedJobs.size()); i++) {
                Map<String, Object> job = recommendedJobs.get(i);
                prompt.append("- ").append(job.get("title")).append(" at ").append(job.get("company_name")).append("\n");
            }
            prompt.append("\n");

            prompt.append("请提供以下三个方面的建议，每个建议1-2句话即可：\n");
            prompt.append("1. 短期职业目标：未来1年内应该如何发展\n");
            prompt.append("2. 技能提升建议：应该学习或提升哪些技能\n");
            prompt.append("3. 长期职业规划：3-5年的职业发展方向\n");
            prompt.append("请按照JSON格式返回，如下所示：\n");
            prompt.append("{\n");
            prompt.append("  \"short_term\": \"短期建议\",\n");
            prompt.append("  \"skills_improvement\": \"技能提升建议\",\n");
            prompt.append("  \"long_term\": \"长期规划\"\n");
            prompt.append("}\n");

            String aiResponse = openAiChatModel.call(prompt.toString());

            try {
                String cleanedResponse = cleanAiResponse(aiResponse);
                return objectMapper.readValue(cleanedResponse, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("职业建议 JSON 解析失败，返回默认建议", e);
                Map<String, Object> fallbackAdvice = new HashMap<>();
                fallbackAdvice.put("short_term", "专注于提升与推荐职位相关的核心技能，参与相关项目积累经验。");
                fallbackAdvice.put("skills_improvement", "关注行业技术趋势，学习推荐职位所需的新兴技能。");
                fallbackAdvice.put("long_term", "逐步积累专业领域影响力，向高级职位或管理岗位发展。");
                return fallbackAdvice;
            }

        } catch (Exception e) {
            logger.error("生成职业发展建议失败", e);
            Map<String, Object> fallbackAdvice = new HashMap<>();
            fallbackAdvice.put("short_term", "专注于提升与推荐职位相关的核心技能，参与相关项目积累经验。");
            fallbackAdvice.put("skills_improvement", "关注行业技术趋势，学习推荐职位所需的新兴技能。");
            fallbackAdvice.put("long_term", "逐步积累专业领域影响力，向高级职位或管理岗位发展。");
            return fallbackAdvice;
        }
    }

    private Map<String, Object> buildNoMatchGuidance(UserResume resume, List<String> skills) {
        Map<String, Object> fallbackGuidance = new HashMap<>();
        fallbackGuidance.put("resume_improvement", "建议补充可量化项目成果，并突出与目标岗位最相关的技术栈和业务场景。");
        fallbackGuidance.put("learning_direction", "建议聚焦岗位高频技能，按“基础知识-实战项目-作品沉淀”路径学习，并定期复盘。");

        try {
            String safeSkills = skills == null || skills.isEmpty() ? "暂无明确技能" : String.join("、", skills);
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一名求职辅导顾问。当前没有匹配岗位，请提供改进建议。\n");
            prompt.append("候选人教育背景：").append(resume.getEducation()).append("\n");
            prompt.append("候选人工作经历：").append(resume.getWork()).append("\n");
            prompt.append("候选人当前技能：").append(safeSkills).append("\n\n");
            prompt.append("请仅返回JSON，结构如下：\n");
            prompt.append("{\n");
            prompt.append("  \"resume_improvement\": \"一句到两句，50字内\",\n");
            prompt.append("  \"learning_direction\": \"一句到两句，50字内\"\n");
            prompt.append("}\n");

            String aiResponse = openAiChatModel.call(prompt.toString());
            String cleanedResponse = cleanAiResponse(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(cleanedResponse, new TypeReference<Map<String, Object>>() {});

            Object resumeImprovement = parsed.get("resume_improvement");
            Object learningDirection = parsed.get("learning_direction");
            if (resumeImprovement instanceof String && !((String) resumeImprovement).trim().isEmpty()) {
                fallbackGuidance.put("resume_improvement", ((String) resumeImprovement).trim());
            }
            if (learningDirection instanceof String && !((String) learningDirection).trim().isEmpty()) {
                fallbackGuidance.put("learning_direction", ((String) learningDirection).trim());
            }
        } catch (Exception e) {
            logger.warn("生成未命中引导建议失败，返回默认建议", e);
        }

        return fallbackGuidance;
    }

    private String cleanAiResponse(String aiResponse) {
        String cleaned = aiResponse.trim();
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private Map<String, Object> extractRecommendationContextFromResume(UserResume resume) {
        Map<String, Object> data = new HashMap<>();

        try {
            if (resume.getProfession() != null && !resume.getProfession().isEmpty()) {
                List<String> skills = extractSkillsFromProfessionJson(resume.getProfession());
                data.put("skills", String.join(", ", skills));
            }

            if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                try {
                    Map<?, ?> educationMap = objectMapper.readValue(resume.getEducation(), Map.class);
                    if (educationMap.get("degree") != null) {
                        data.put("educationLevel", educationMap.get("degree").toString());
                    }
                } catch (Exception e) {
                    try {
                        List<?> educationList = objectMapper.readValue(resume.getEducation(), List.class);
                        if (!educationList.isEmpty() && educationList.get(0) instanceof Map) {
                            Map<?, ?> firstEducation = (Map<?, ?>) educationList.get(0);
                            if (firstEducation.get("degree") != null) {
                                data.put("educationLevel", firstEducation.get("degree").toString());
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }

            if (resume.getWork() != null && !resume.getWork().isEmpty()) {
                try {
                    Map<?, ?> workMap = objectMapper.readValue(resume.getWork(), Map.class);
                    if (workMap.containsKey("period") && workMap.get("period") instanceof List) {
                        List<?> periodList = (List<?>) workMap.get("period");
                        if (periodList.size() >= 2) {
                            data.put("experience", periodList.size() / 2);
                        }
                    }
                } catch (Exception e) {
                    try {
                        List<?> workList = objectMapper.readValue(resume.getWork(), List.class);
                        if (!workList.isEmpty()) {
                            data.put("experience", workList.size());
                        } else {
                            data.put("experience", 0);
                        }
                    } catch (Exception ex) {
                        data.put("experience", 0);
                    }
                }
            } else {
                data.put("experience", 0);
            }

            if (resume.getJobStatus() != null && isLikelyLocationText(resume.getJobStatus())) {
                data.put("location", resume.getJobStatus().trim());
            }

        } catch (Exception e) {
            logger.warn("提取简历推荐上下文失败，返回空上下文", e);
        }

        return data;
    }

    private List<String> extractSkillsFromProfessionJson(String professionJson) {
        List<String> skills = new ArrayList<>();

        try {
            try {
                List<?> professionList = objectMapper.readValue(professionJson, List.class);
                for (Object item : professionList) {
                    if (item instanceof Map) {
                        Map<?, ?> professionItem = (Map<?, ?>) item;
                        if (professionItem.containsKey("content")) {
                            skills.add(professionItem.get("content").toString());
                        }
                    } else if (item instanceof String) {
                        skills.add(item.toString());
                    }
                }
            } catch (Exception e) {
                try {
                    Map<?, ?> professionMap = objectMapper.readValue(professionJson, Map.class);
                    if (professionMap.containsKey("skill")) {
                        String skillText = professionMap.get("skill").toString();
                        String[] skillArray = skillText.split("[,;，；、\n]");
                        skills.addAll(Arrays.asList(skillArray));
                    }
                } catch (Exception ex) {
                    String[] skillArray = professionJson.split("[,;，；、\n]");
                    skills.addAll(Arrays.asList(skillArray));
                }
            }

            skills = skills.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (skills.isEmpty()) {
                skills.add("通用技能");
            }

        } catch (Exception e) {
            logger.warn("技能提取失败，回退为通用技能", e);
            skills.add("通用技能");
        }

        return skills;
    }

    private List<String> expandSkillKeywords(List<String> rawSkills) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();

        for (String raw : rawSkills) {
            if (raw == null) {
                continue;
            }
            String normalized = raw.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            expanded.add(normalized);

            String[] parts = normalized.split("[,;，；、()（）/\\n\\s]+");
            for (String part : parts) {
                String token = part.trim();
                if (!token.isEmpty() && token.length() >= 2 && token.length() <= 30) {
                    expanded.add(token);
                }
            }

            String lower = normalized.toLowerCase();
            if (lower.contains("java")) expanded.add("java");
            if (lower.contains("java ee") || lower.contains("j2ee")) expanded.add("java ee");
            if (lower.contains("spring")) expanded.add("spring");
            if (lower.contains("spring boot")) expanded.add("spring boot");
            if (lower.contains("spring mvc")) expanded.add("spring mvc");
            if (lower.contains("hibernate")) expanded.add("hibernate");
            if (lower.contains("restful")) expanded.add("restful");
            if (lower.contains("api")) expanded.add("api");
            if (lower.contains("微服务")) expanded.add("微服务");
            if (lower.contains("mysql")) expanded.add("mysql");
            if (lower.contains("oracle")) expanded.add("oracle");
            if (lower.contains("git")) expanded.add("git");
            if (lower.contains("单元测试")) expanded.add("单元测试");
            if (lower.contains("自动化测试")) expanded.add("自动化测试");
            if (lower.contains("scrum")) expanded.add("scrum");
            if (lower.contains("kanban")) expanded.add("kanban");
        }

        return expanded.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isLikelyLocationText(String value) {
        if (value == null) {
            return false;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return false;
        }
        return text.contains("市")
                || text.contains("省")
                || text.contains("区")
                || text.contains("县")
                || text.contains("北京")
                || text.contains("上海")
                || text.contains("广州")
                || text.contains("深圳")
                || text.contains("杭州")
                || text.contains("南京")
                || text.contains("成都")
                || text.contains("武汉");
    }

    private boolean isLocationMatched(String candidateLocation, String jobLocation) {
        if (candidateLocation == null || candidateLocation.trim().isEmpty()) {
            return false;
        }
        if (jobLocation == null || jobLocation.trim().isEmpty()) {
            return false;
        }
        String candidate = candidateLocation.trim().toLowerCase();
        String job = jobLocation.trim().toLowerCase();
        return job.contains(candidate) || candidate.contains(job);
    }

    private int calculateJobMatchScore(String requirements, List<String> skills, String educationLevel,
                                   Integer experience, String candidateLocation, String jobLocation) {
        int score = 0;

        if (requirements != null) {
            for (String skill : skills) {
                if (requirements.toLowerCase().contains(skill.toLowerCase())) {
                    score += 10;
                }
            }
        }

        if (educationLevel != null && requirements != null
            && requirements.toLowerCase().contains(educationLevel.toLowerCase())) {
            score += 15;
        }

        if (experience != null && requirements != null) {
            String lowerReq = requirements.toLowerCase();
            if (lowerReq.contains(experience + " 年")
                || lowerReq.contains(experience + "+ 年")
                || lowerReq.contains("低于 " + (experience + 1) + " 年")) {
                score += 20;
            }
        }

        if (isLocationMatched(candidateLocation, jobLocation)) {
            score += 15;
        }

        return score;
    }
}
