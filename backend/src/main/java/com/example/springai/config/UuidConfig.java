package com.example.springai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidConfig {

    /**
     * 提供统一 UUID 生成器 Bean。
     */
    @Bean
    public UuidGenerator uuidGenerator() {
        return new UuidGenerator();
    }

    public static class UuidGenerator {
        public String generateUuid32() {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
    }
}