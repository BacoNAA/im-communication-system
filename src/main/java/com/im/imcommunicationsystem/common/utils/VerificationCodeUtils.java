package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.common.config.VerificationCodeConfig;
import com.im.imcommunicationsystem.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 * 用于生成、存储和验证各种类型的验证码
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class VerificationCodeUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private VerificationCodeConfig verificationCodeConfig;

    /**
     * 数字字符集
     */
    private static final String NUMBERS = "0123456789";

    /**
     * 字母字符集
     */
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 数字和字母字符集
     */
    private static final String ALPHANUMERIC = NUMBERS + LETTERS;

    /**
     * Redis键前缀
     */
    private static final String REDIS_KEY_PREFIX = "verification_code:";
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 验证码类型枚举
     */
    public enum CodeType {
        /**
         * 邮箱注册验证码
         */
        EMAIL_REGISTER("email:register", 6, 5, NUMBERS),
        
        /**
         * 邮箱登录验证码
         */
        EMAIL_LOGIN("email:login", 6, 5, NUMBERS),
        
        /**
         * 密码重置验证码
         */
        PASSWORD_RESET("password:reset", 6, 10, NUMBERS),
        
        /**
         * 邮箱绑定验证码
         */
        EMAIL_BIND("email:bind", 6, 5, NUMBERS),
        
        /**
         * 邮箱解绑验证码
         */
        EMAIL_UNBIND("email:unbind", 6, 5, NUMBERS),
        
        /**
         * 手机号验证码
         */
        PHONE_VERIFY("phone:verify", 6, 5, NUMBERS),
        
        /**
         * 图形验证码
         */
        CAPTCHA("captcha", 4, 2, ALPHANUMERIC),
        
        /**
         * 二次验证码
         */
        TWO_FACTOR("two:factor", 6, 10, NUMBERS);

        private final String prefix;
        private final int length;
        private final int expireMinutes;
        private final String charset;

        CodeType(String prefix, int length, int expireMinutes, String charset) {
            this.prefix = prefix;
            this.length = length;
            this.expireMinutes = expireMinutes;
            this.charset = charset;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getLength() {
            return length;
        }

        public int getExpireMinutes() {
            return expireMinutes;
        }

        public String getCharset() {
            return charset;
        }
    }

    /**
     * 生成验证码
     * 
     * @param identifier 标识符（邮箱、手机号等）
     * @param codeType 验证码类型
     * @return 验证码
     */
    public String generateCode(String identifier, CodeType codeType) {
        // 参数验证
        validateParameters(identifier, codeType);
        
        // 检查发送频率限制
        checkSendFrequency(identifier, codeType);
        
        try {
            // 生成验证码
            String code = generateCodeByType(codeType);
            
            // 构建Redis键
            String key = buildRedisKey(identifier, codeType);
            
            // 存储到Redis
            redisTemplate.opsForValue().set(key, code, codeType.getExpireMinutes(), TimeUnit.MINUTES);
            
            log.info("生成验证码成功: identifier={}, type={}, length={}, expireMinutes={}", 
                    identifier, codeType.name(), code.length(), codeType.getExpireMinutes());
            
            return code;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法生成验证码: identifier={}, type={}", identifier, codeType.name(), e);
            throw ServiceException.redisConnectionError("验证码生成失败，缓存服务不可用", e);
        } catch (Exception e) {
            log.error("生成验证码时发生未知错误: identifier={}, type={}", identifier, codeType.name(), e);
            throw ServiceException.verificationCodeError("验证码生成失败: " + e.getMessage());
        }
    }

    /**
     * 验证验证码
     * 
     * @param identifier 标识符
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 是否验证成功
     */
    public boolean verifyCode(String identifier, String code, CodeType codeType) {
        // 参数验证
        validateVerifyParameters(identifier, code, codeType);
        
        // 检查重试次数限制
        checkRetryLimit(identifier, codeType);
        
        try {
            // 构建Redis键
            String key = buildRedisKey(identifier, codeType);
            
            // 从Redis获取验证码
            String storedCode = (String) redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.warn("验证码不存在或已过期: identifier={}, type={}", identifier, codeType.name());
                incrementRetryCount(identifier, codeType);
                return false;
            }
            
            // 验证码比较
            boolean isValid = code.trim().equalsIgnoreCase(storedCode);
            
            if (isValid) {
                // 验证成功，删除验证码和重试计数
                redisTemplate.delete(key);
                clearRetryCount(identifier, codeType);
                log.info("验证码验证成功: identifier={}, type={}", identifier, codeType.name());
            } else {
                // 验证失败，增加重试计数
                incrementRetryCount(identifier, codeType);
                log.warn("验证码验证失败: identifier={}, type={}, inputCode={}", 
                        identifier, codeType.name(), code);
            }
            
            return isValid;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法验证验证码: identifier={}, type={}", identifier, codeType.name(), e);
            throw ServiceException.redisConnectionError("验证码验证失败，缓存服务不可用", e);
        } catch (Exception e) {
            log.error("验证验证码时发生未知错误: identifier={}, type={}", identifier, codeType.name(), e);
            throw ServiceException.verificationCodeError("验证码验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查验证码是否存在
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     * @return 是否存在
     */
    public boolean codeExists(String identifier, CodeType codeType) {
        String key = buildRedisKey(identifier, codeType);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取验证码剩余有效时间（秒）
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     * @return 剩余有效时间
     */
    public long getCodeRemainingTime(String identifier, CodeType codeType) {
        String key = buildRedisKey(identifier, codeType);
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? Math.max(0, expire) : 0;
    }

    /**
     * 删除验证码
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     */
    public void deleteCode(String identifier, CodeType codeType) {
        String key = buildRedisKey(identifier, codeType);
        redisTemplate.delete(key);
        log.info("删除验证码: {}", identifier);
    }

    /**
     * 检查是否可以发送验证码（防止频繁发送）
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     * @param intervalSeconds 发送间隔（秒）
     * @return 是否可以发送
     */
    public boolean canSendCode(String identifier, CodeType codeType, int intervalSeconds) {
        try {
            long remainingTime = getCodeRemainingTime(identifier, codeType);
            
            if (remainingTime > 0) {
                long elapsedTime = (codeType.getExpireMinutes() * 60) - remainingTime;
                return elapsedTime >= intervalSeconds;
            }
            
            return true;
            
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，无法检查发送频率: identifier={}, type={}", identifier, codeType.name(), e);
            // Redis连接失败时，为了安全起见，不允许发送
            return false;
        } catch (Exception e) {
            log.error("检查发送频率时发生未知错误: identifier={}, type={}", identifier, codeType.name(), e);
            return false;
        }
    }

    /**
     * 参数验证
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     */
    private void validateParameters(String identifier, CodeType codeType) {
        if (!StringUtils.hasText(identifier)) {
            throw ServiceException.validationError("标识符不能为空", null);
        }
        
        if (codeType == null) {
            throw ServiceException.validationError("验证码类型不能为空", null);
        }
        
        // 验证标识符格式（可根据类型进行不同验证）
        if (identifier.trim().length() < 3) {
            throw ServiceException.validationError("标识符长度不能少于3位", identifier);
        }
    }
    
    /**
     * 检查发送频率限制
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     */
    private void checkSendFrequency(String identifier, CodeType codeType) {
        if (!canSendCode(identifier, codeType, verificationCodeConfig.getSendIntervalSeconds())) {
            long remainingTime = getCodeRemainingTime(identifier, codeType);
            long waitTime = verificationCodeConfig.getSendIntervalSeconds() - 
                    ((codeType.getExpireMinutes() * 60) - remainingTime);
            throw ServiceException.rateLimitExceeded("验证码发送", Math.max(waitTime, 0));
        }
    }
    
    /**
     * 根据类型生成验证码
     * 
     * @param codeType 验证码类型
     * @return 验证码
     */
    private String generateCodeByType(CodeType codeType) {
        String code;
        switch (codeType) {
            case CAPTCHA:
                code = generateRandomCode(codeType.getLength(), ALPHANUMERIC);
                break;
            default:
                code = generateRandomCode(codeType.getLength(), NUMBERS);
                break;
        }
        
        // 验证码复杂度检查（如果启用）
        if (verificationCodeConfig.getSecurity().isEnableComplexityCheck()) {
            validateCodeComplexity(code, codeType);
        }
        
        return code;
    }
    
    /**
      * 验证码复杂度检查
      * 
      * @param code 验证码
      * @param codeType 验证码类型
      */
     private void validateCodeComplexity(String code, CodeType codeType) {
         // 对于字母数字验证码，确保包含数字和字母
         if (codeType == CodeType.CAPTCHA && code.length() >= 4) {
             boolean hasDigit = code.chars().anyMatch(Character::isDigit);
             boolean hasLetter = code.chars().anyMatch(Character::isLetter);
             
             if (!hasDigit || !hasLetter) {
                 // 重新生成
                 throw new RuntimeException("验证码复杂度不足，需要重新生成");
             }
         }
     }
     
     /**
      * 验证验证参数
      * 
      * @param identifier 标识符
      * @param code 验证码
      * @param codeType 验证码类型
      */
     private void validateVerifyParameters(String identifier, String code, CodeType codeType) {
         validateParameters(identifier, codeType);
         
         if (!StringUtils.hasText(code)) {
             throw ServiceException.validationError("验证码不能为空", null);
         }
         
         String trimmedCode = code.trim();
         
         if (trimmedCode.length() != codeType.getLength()) {
             throw ServiceException.validationError("验证码长度不正确", code);
         }
         
         // 对于数字验证码类型，严格验证只能包含数字
         if (codeType == CodeType.EMAIL_REGISTER || codeType == CodeType.EMAIL_LOGIN || 
             codeType == CodeType.PASSWORD_RESET || codeType == CodeType.EMAIL_BIND || 
             codeType == CodeType.PHONE_VERIFY || codeType == CodeType.TWO_FACTOR) {
             
             if (!trimmedCode.matches("\\d+")) {
                 throw ServiceException.validationError("验证码格式不正确，只能包含数字", code);
             }
         }
     }
     
     /**
      * 检查重试次数限制
      * 
      * @param identifier 标识符
      * @param codeType 验证码类型
      */
     private void checkRetryLimit(String identifier, CodeType codeType) {
         String retryKey = buildRetryKey(identifier, codeType);
         String retryCountStr = (String) redisTemplate.opsForValue().get(retryKey);
         
         if (retryCountStr != null) {
             int retryCount = Integer.parseInt(retryCountStr);
             if (retryCount >= verificationCodeConfig.getMaxRetryCount()) {
                 long ttl = redisTemplate.getExpire(retryKey, TimeUnit.SECONDS);
                 throw ServiceException.rateLimitExceeded("验证码重试", Math.max(ttl, 0));
             }
         }
     }
     
     /**
      * 增加重试计数
      * 
      * @param identifier 标识符
      * @param codeType 验证码类型
      */
     private void incrementRetryCount(String identifier, CodeType codeType) {
         String retryKey = buildRetryKey(identifier, codeType);
         String retryCountStr = (String) redisTemplate.opsForValue().get(retryKey);
         
         int retryCount = retryCountStr != null ? Integer.parseInt(retryCountStr) + 1 : 1;
         
         // 设置重试计数，过期时间为验证码过期时间
         redisTemplate.opsForValue().set(retryKey, String.valueOf(retryCount), 
                 codeType.getExpireMinutes(), TimeUnit.MINUTES);
     }
     
     /**
      * 清除重试计数
      * 
      * @param identifier 标识符
      * @param codeType 验证码类型
      */
     private void clearRetryCount(String identifier, CodeType codeType) {
         String retryKey = buildRetryKey(identifier, codeType);
         redisTemplate.delete(retryKey);
     }
     
     /**
      * 构建重试计数Redis键
      * 
      * @param identifier 标识符
      * @param codeType 验证码类型
      * @return Redis键
      */
     private String buildRetryKey(String identifier, CodeType codeType) {
         return REDIS_KEY_PREFIX + "retry:" + codeType.name().toLowerCase() + ":" + identifier;
     }

    /**
     * 生成随机验证码
     * 
     * @param length 长度
     * @param charset 字符集
     * @return 验证码
     */
    private String generateRandomCode(int length, String charset) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(charset.length());
            code.append(charset.charAt(index));
        }
        return code.toString();
    }

    /**
     * 构建Redis键
     * 
     * @param identifier 标识符
     * @param codeType 验证码类型
     * @return Redis键
     */
    private String buildRedisKey(String identifier, CodeType codeType) {
        return String.format("verification:code:%s:%s", codeType.getPrefix(), identifier);
    }

    /**
     * 生成数字验证码
     * 
     * @param identifier 标识符
     * @param length 长度
     * @param expireMinutes 过期时间（分钟）
     * @return 验证码
     */
    public String generateNumericCode(String identifier, int length, int expireMinutes) {
        String code = generateRandomCode(length, NUMBERS);
        redisTemplate.opsForValue().set("verification:code:custom:" + identifier, code, expireMinutes, TimeUnit.MINUTES);
        return code;
    }

    /**
     * 生成字母数字验证码
     * 
     * @param identifier 标识符
     * @param length 长度
     * @param expireMinutes 过期时间（分钟）
     * @return 验证码
     */
    public String generateAlphanumericCode(String identifier, int length, int expireMinutes) {
        String code = generateRandomCode(length, ALPHANUMERIC);
        redisTemplate.opsForValue().set("verification:code:custom:" + identifier, code, expireMinutes, TimeUnit.MINUTES);
        return code;
    }

    /**
     * 验证码使用说明：
     * 
     * 1. 验证码类型：
     *    - EMAIL_REGISTER: 邮箱注册验证码，6位数字，5分钟有效
     *    - EMAIL_LOGIN: 邮箱登录验证码，6位数字，5分钟有效
     *    - PASSWORD_RESET: 密码重置验证码，6位数字，10分钟有效
     *    - EMAIL_BIND: 邮箱绑定验证码，6位数字，5分钟有效
     *    - PHONE_VERIFY: 手机验证码，6位数字，5分钟有效
     *    - CAPTCHA: 图形验证码，4位字母数字，2分钟有效
     *    - TWO_FACTOR: 二次验证码，6位数字，10分钟有效
     * 
     * 2. Redis存储格式：
     *    - Key: verification:code:{type}:{identifier}
     *    - Value: 验证码字符串
     *    - TTL: 根据类型设置过期时间
     * 
     * 3. 安全特性：
     *    - 使用SecureRandom生成随机数
     *    - 验证成功后自动删除验证码
     *    - 支持发送频率限制
     *    - 大小写不敏感验证
     * 
     * 4. 使用示例：
     *    // 生成邮箱注册验证码
     *    String code = generateCode("user@example.com", CodeType.EMAIL_REGISTER);
     *    
     *    // 验证验证码
     *    boolean isValid = verifyCode("user@example.com", "123456", CodeType.EMAIL_REGISTER);
     *    
     *    // 检查是否可以发送（60秒间隔）
     *    boolean canSend = canSendCode("user@example.com", CodeType.EMAIL_REGISTER, 60);
     * 
     * 5. 错误处理：
     *    - 验证码不存在或已过期
     *    - 验证码格式错误
     *    - 发送频率过高
     *    - Redis连接异常
     */

}