package com.im.imcommunicationsystem.controller;

import com.im.imcommunicationsystem.auth.service.AccountLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/account-lock")
@Slf4j
public class AccountLockTestController {

    @Autowired
    private AccountLockService accountLockService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/test-failure/{email}")
    public ResponseEntity<Map<String, Object>> testLoginFailure(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始记录登录失败: {}", email);
            // 测试记录登录失败
            int attempts = accountLockService.recordLoginFailure(email);
            boolean isLocked = accountLockService.isAccountLocked(email);
            int remaining = accountLockService.getRemainingAttempts(email);
            
            log.info("登录失败记录完成，当前失败次数: {}, 是否锁定: {}, 剩余尝试次数: {}", attempts, isLocked, remaining);
            
            result.put("status", "success");
            result.put("email", email);
            result.put("attempts", attempts);
            result.put("isLocked", isLocked);
            result.put("remainingAttempts", remaining);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("记录登录失败时发生错误: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @GetMapping("/status/{email}")
    public ResponseEntity<Map<String, Object>> getAccountStatus(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("获取账户状态: {}", email);
            int attempts = accountLockService.getLoginAttempts(email);
            boolean isLocked = accountLockService.isAccountLocked(email);
            int remaining = accountLockService.getRemainingAttempts(email);
            long lockTime = accountLockService.getAccountLockRemainingTime(email);
            
            log.info("账户状态 - 失败次数: {}, 是否锁定: {}, 剩余尝试: {}, 锁定剩余时间: {}", attempts, isLocked, remaining, lockTime);
            
            result.put("status", "success");
            result.put("email", email);
            result.put("attempts", attempts);
            result.put("isLocked", isLocked);
            result.put("remainingAttempts", remaining);
            result.put("lockRemainingTime", lockTime);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取账户状态时发生错误: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "获取状态失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @DeleteMapping("/clear/{email}")
    public ResponseEntity<Map<String, Object>> clearAccount(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            accountLockService.clearLoginFailures(email);
            accountLockService.unlockAccount(email);
            
            result.put("status", "success");
            result.put("message", "账户状态已清除");
            result.put("email", email);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "清除失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @GetMapping("/redis-keys")
    public ResponseEntity<Map<String, Object>> getRedisKeys() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("获取Redis键信息");
            // 获取所有相关的Redis键
            var keys = redisTemplate.keys("login_attempts:*");
            var lockKeys = redisTemplate.keys("account_lock:*");
            
            log.info("找到的Redis键 - 登录尝试键: {}, 锁定键: {}", keys, lockKeys);
            
            result.put("status", "success");
            result.put("attemptKeys", keys);
            result.put("lockKeys", lockKeys);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取Redis键时发生错误: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "获取Redis键失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @GetMapping("/test-redis")
    public ResponseEntity<Map<String, Object>> testRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("测试Redis连接");
            // 测试Redis连接
            String testKey = "test_connection";
            String testValue = "test_value_" + System.currentTimeMillis();
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            boolean connectionOk = testValue.equals(retrievedValue);
            log.info("Redis连接测试结果: {}", connectionOk);
            
            result.put("status", "success");
            result.put("connectionOk", connectionOk);
            result.put("testValue", testValue);
            result.put("retrievedValue", retrievedValue);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Redis连接测试失败: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Redis连接测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(result);
        }
    }
}