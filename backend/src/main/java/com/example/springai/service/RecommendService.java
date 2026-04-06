package com.example.springai.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface RecommendService {

    ResponseEntity<?> recommendJobs(Map<String, Object> requestPayload);
}
