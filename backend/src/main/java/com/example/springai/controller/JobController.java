package com.example.springai.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springai.model.JobCategory;
import com.example.springai.model.JobPosition;
import com.example.springai.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job")
@CrossOrigin
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping("/categories")
    public Map<String, Object> getCategories() {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            List<JobCategory> categoryList = jobService.listAllCategories();
            responseBody.put("success", true);
            responseBody.put("data", categoryList);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @GetMapping("/list")
    public Map<String, Object> getJobList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId
    ) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            IPage<JobPosition> pageResult = jobService.queryJobPage(page, size, keyword, categoryId);

            Map<String, Object> data = new HashMap<>();
            data.put("list", pageResult.getRecords());
            data.put("total", pageResult.getTotal());

            responseBody.put("success", true);
            responseBody.put("data", data);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> getJobDetail(@PathVariable Long id) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            JobPosition jobDetail = jobService.findJobById(id);
            responseBody.put("success", true);
            responseBody.put("data", jobDetail);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @PostMapping("/add")
    public Map<String, Object> addJob(@RequestBody JobPosition jobPayload) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            boolean success = jobService.createJob(jobPayload);
            responseBody.put("success", success);
            if (success) {
                responseBody.put("data", jobPayload);
            }
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @PostMapping("/update")
    public Map<String, Object> updateJob(@RequestBody JobPosition jobPayload) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            boolean success = jobService.updateJob(jobPayload);
            responseBody.put("success", success);
            if (success) {
                responseBody.put("data", jobPayload);
            }
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteJob(@PathVariable Long id) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            boolean success = jobService.removeJob(id);
            responseBody.put("success", success);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    // 职位分类相关接口
    @PostMapping("/category/add")
    public Map<String, Object> addCategory(@RequestBody JobCategory categoryPayload) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            JobCategory savedCategory = jobService.createCategory(categoryPayload);
            responseBody.put("success", true);
            responseBody.put("data", savedCategory);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }

    @DeleteMapping("/category/delete/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Integer id) {
        Map<String, Object> responseBody = new HashMap<>();
        try {
            boolean success = jobService.removeCategory(id);
            responseBody.put("success", success);
        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("error", e.getMessage());
        }
        return responseBody;
    }
}
