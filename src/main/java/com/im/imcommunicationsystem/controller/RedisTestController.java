package com.im.imcommunicationsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> testRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试Redis连接
            redisTemplate.opsForValue().set("test:ping", "pong");
            String value = (String) redisTemplate.opsForValue().get("test:ping");
            
            result.put("status", "success");
            result.put("message", "Redis连接正常");
            result.put("testValue", value);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getRedisInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取Redis信息
            result.put("status", "success");
            result.put("message", "Redis信息获取成功");
            result.put("connectionFactory", redisTemplate.getConnectionFactory().getClass().getSimpleName());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取Redis信息失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
}