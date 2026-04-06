package com.example.springai.controller;

import com.example.springai.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
@CrossOrigin
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @PostMapping("/jobs")
    public ResponseEntity<?> recommendJobs(@RequestBody Map<String, Object> requestPayload) {
        return recommendService.recommendJobs(requestPayload);
    }
}
