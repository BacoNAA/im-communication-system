package com.im.imcommunicationsystem.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 账户锁定服务
 * 管理登录失败次数和账户锁定状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 最大登录失败次数
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    
    // 账户锁定时间（分钟）
    private static final int LOCK_DURATION_MINUTES = 30;
    
    // Redis键前缀
    private static final String LOGIN_ATTEMPTS_KEY_PREFIX = "login_attempts:";
    private static final String ACCOUNT_LOCK_KEY_PREFIX = "account_lock:";
    
    /**
     * 记录登录失败
     * @param email 用户邮箱
     * @return 当前失败次数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordLoginFailure(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.error("记录登录失败时邮箱为空");
            throw new IllegalArgumentException("邮箱不能为空");
        }
        
        String key = LOGIN_ATTEMPTS_KEY_PREFIX + email;
        
        try {
            // 获取当前失败次数
            Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
            if (attempts == null) {
                attempts = 0;
            }
            
            // 如果账户已经被锁定，直接返回当前失败次数，不再增加
            if (isAccountLocked(email)) {
                long remainingTime = getAccountLockRemainingTime(email);
                long remainingMinutes = remainingTime / 60;
                log.warn("用户 {} 账户已锁定，当前失败次数: {}，剩余锁定时间: {} 分钟", email, attempts, remainingMinutes);
                return attempts;
            }
            
            attempts++;
            
            // 设置失败次数，1小时后过期
            redisTemplate.opsForValue().set(key, attempts, Duration.ofHours(1));
            
            log.warn("用户 {} 登录失败，当前失败次数: {}，剩余可尝试次数: {}", 
                    email, attempts, Math.max(0, MAX_LOGIN_ATTEMPTS - attempts));
            
            // 如果达到最大失败次数，锁定账户
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                lockAccount(email);
                log.error("用户 {} 因连续 {} 次登录失败被锁定账户", email, attempts);
            }
            
            return attempts;
        } catch (Exception e) {
            log.error("记录用户 {} 登录失败时发生异常", email, e);
            throw new RuntimeException("记录登录失败状态时发生系统错误", e);
        }
    }
    
    /**
     * 清除登录失败记录
     * @param email 用户邮箱
     */
    public void clearLoginFailures(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("清除登录失败记录时邮箱为空");
            return;
        }
        
        String key = LOGIN_ATTEMPTS_KEY_PREFIX + email;
        
        try {
            int previousAttempts = getLoginAttempts(email);
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.info("成功清除用户 {} 的登录失败记录，之前失败次数: {}", email, previousAttempts);
            } else {
                log.info("用户 {} 无登录失败记录需要清除", email);
            }
        } catch (Exception e) {
            log.error("清除用户 {} 登录失败记录时发生异常", email, e);
            throw new RuntimeException("清除登录失败记录时发生系统错误", e);
        }
    }
    
    /**
     * 锁定账户
     * @param email 用户邮箱
     */
    public void lockAccount(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.error("锁定账户时邮箱为空");
            throw new IllegalArgumentException("邮箱不能为空");
        }
        
        String key = ACCOUNT_LOCK_KEY_PREFIX + email;
        
        try {
            // 设置锁定标记，指定时间后过期
            redisTemplate.opsForValue().set(key, true, Duration.ofMinutes(LOCK_DURATION_MINUTES));
            
            log.error("安全警告：账户 {} 因多次登录失败已被锁定 {} 分钟，锁定时间至: {}", 
                    email, LOCK_DURATION_MINUTES, 
                    java.time.LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        } catch (Exception e) {
            log.error("锁定账户 {} 时发生异常", email, e);
            throw new RuntimeException("锁定账户时发生系统错误", e);
        }
    }
    
    /**
     * 检查账户是否被锁定
     * @param email 用户邮箱
     * @return 是否被锁定
     */
    public boolean isAccountLocked(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("检查账户锁定状态时邮箱为空");
            return false;
        }
        
        String key = ACCOUNT_LOCK_KEY_PREFIX + email;
        
        try {
            Boolean locked = (Boolean) redisTemplate.opsForValue().get(key);
            boolean isLocked = locked != null && locked;
            
            if (isLocked) {
                long remainingTime = getAccountLockRemainingTime(email);
                log.info("账户 {} 当前处于锁定状态，剩余锁定时间: {} 秒", email, remainingTime);
            }
            
            return isLocked;
        } catch (Exception e) {
            log.error("检查账户 {} 锁定状态时发生异常", email, e);
            // 发生异常时为了安全起见，假设账户未锁定，让后续逻辑处理
            return false;
        }
    }
    
    /**
     * 获取账户锁定剩余时间（秒）
     * @param email 用户邮箱
     * @return 剩余锁定时间，如果未锁定返回0
     */
    public long getAccountLockRemainingTime(String email) {
        String key = ACCOUNT_LOCK_KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }
    
    /**
     * 获取当前登录失败次数
     * @param email 用户邮箱
     * @return 失败次数
     */
    public int getLoginAttempts(String email) {
        String key = LOGIN_ATTEMPTS_KEY_PREFIX + email;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0;
    }
    
    /**
     * 解锁账户
     * @param email 用户邮箱
     */
    public void unlockAccount(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.error("解锁账户时邮箱为空");
            throw new IllegalArgumentException("邮箱不能为空");
        }
        
        String lockKey = ACCOUNT_LOCK_KEY_PREFIX + email;
        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + email;
        
        try {
            boolean wasLocked = isAccountLocked(email);
            int previousAttempts = getLoginAttempts(email);
            
            Boolean deletedLock = redisTemplate.delete(lockKey);
            Boolean deletedAttempts = redisTemplate.delete(attemptsKey);
            
            if (wasLocked) {
                log.info("管理员手动解锁账户 {}，之前失败次数: {}，锁定状态已清除", email, previousAttempts);
            } else {
                log.info("解锁账户 {}，该账户之前未被锁定，已清除失败记录: {}", email, previousAttempts);
            }
        } catch (Exception e) {
            log.error("解锁账户 {} 时发生异常", email, e);
            throw new RuntimeException("解锁账户时发生系统错误", e);
        }
    }
    
    /**
     * 获取剩余可尝试次数
     * @param email 用户邮箱
     * @return 剩余次数
     */
    public int getRemainingAttempts(String email) {
        int currentAttempts = getLoginAttempts(email);
        return Math.max(0, MAX_LOGIN_ATTEMPTS - currentAttempts);
    }
}