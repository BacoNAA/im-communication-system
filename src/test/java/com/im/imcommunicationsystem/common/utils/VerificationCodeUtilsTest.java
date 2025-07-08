package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.common.config.VerificationCodeConfig;
import com.im.imcommunicationsystem.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证码工具类测试
 * 
 * @author System
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationCodeUtilsTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private VerificationCodeConfig verificationCodeConfig;
    
    @Mock
    private VerificationCodeConfig.SecurityConfig securityConfig;
    
    @InjectMocks
    private VerificationCodeUtils verificationCodeUtils;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(verificationCodeConfig.getSendIntervalSeconds()).thenReturn(60);
        when(verificationCodeConfig.getMaxRetryCount()).thenReturn(5);
        when(verificationCodeConfig.getSecurity()).thenReturn(securityConfig);
        when(securityConfig.isEnableComplexityCheck()).thenReturn(false);
    }
    
    @Test
    void testGenerateCode_Success() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(valueOperations.get(anyString())).thenReturn(null); // 没有现有验证码
        
        // When
        String code = verificationCodeUtils.generateCode(identifier, codeType);
        
        // Then
        assertNotNull(code);
        assertEquals(codeType.getLength(), code.length());
        assertTrue(code.matches("\\d+")); // 数字验证码
        
        verify(valueOperations).set(anyString(), eq(code), eq((long) codeType.getExpireMinutes()), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testGenerateCode_InvalidIdentifier() {
        // Given
        String identifier = "";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        // When & Then
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> verificationCodeUtils.generateCode(identifier, codeType));
        
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("标识符不能为空"));
    }
    
    @Test
    void testGenerateCode_NullCodeType() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = null;
        
        // When & Then
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> verificationCodeUtils.generateCode(identifier, codeType));
        
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("验证码类型不能为空"));
    }
    
    @Test
    void testVerifyCode_Success() {
        // Given
        String identifier = "test@example.com";
        String code = "123456";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(valueOperations.get(anyString())).thenReturn(code); // 返回存储的验证码
        when(valueOperations.get(contains("retry"))).thenReturn(null); // 没有重试记录
        
        // When
        boolean result = verificationCodeUtils.verifyCode(identifier, code, codeType);
        
        // Then
        assertTrue(result);
        verify(redisTemplate, times(2)).delete(anyString()); // 删除验证码和重试计数
    }
    
    @Test
    void testVerifyCode_WrongCode() {
        // Given
        String identifier = "test@example.com";
        String inputCode = "123456";
        String storedCode = "654321";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(valueOperations.get(anyString())).thenReturn(storedCode);
        when(valueOperations.get(contains("retry"))).thenReturn(null);
        
        // When
        boolean result = verificationCodeUtils.verifyCode(identifier, inputCode, codeType);
        
        // Then
        assertFalse(result);
        verify(valueOperations).set(contains("retry"), eq("1"), eq((long) codeType.getExpireMinutes()), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testVerifyCode_ExpiredCode() {
        // Given
        String identifier = "test@example.com";
        String code = "123456";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(valueOperations.get(anyString())).thenReturn(null); // 验证码已过期
        when(valueOperations.get(contains("retry"))).thenReturn(null);
        
        // When
        boolean result = verificationCodeUtils.verifyCode(identifier, code, codeType);
        
        // Then
        assertFalse(result);
        verify(valueOperations).set(contains("retry"), eq("1"), eq((long) codeType.getExpireMinutes()), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testVerifyCode_ExceedRetryLimit() {
        // Given
        String identifier = "test@example.com";
        String code = "123456";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(valueOperations.get(contains("retry"))).thenReturn("5"); // 已达到重试上限
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L);
        
        // When & Then
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> verificationCodeUtils.verifyCode(identifier, code, codeType));
        
        assertEquals("RATE_LIMIT_EXCEEDED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("验证码重试"));
    }
    
    @Test
    void testCanSendCode_AllowSend() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        int intervalSeconds = 60;
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // 没有现有验证码
        
        // When
        boolean canSend = verificationCodeUtils.canSendCode(identifier, codeType, intervalSeconds);
        
        // Then
        assertTrue(canSend);
    }
    
    @Test
    void testCanSendCode_TooFrequent() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        int intervalSeconds = 60;
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(540L); // 还有9分钟过期
        
        // When
        boolean canSend = verificationCodeUtils.canSendCode(identifier, codeType, intervalSeconds);
        
        // Then
        assertFalse(canSend); // 发送太频繁
    }
    
    @Test
    void testCodeExists() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        // When
        boolean exists = verificationCodeUtils.codeExists(identifier, codeType);
        
        // Then
        assertTrue(exists);
    }
    
    @Test
    void testGetCodeRemainingTime() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L);
        
        // When
        long remainingTime = verificationCodeUtils.getCodeRemainingTime(identifier, codeType);
        
        // Then
        assertEquals(300L, remainingTime);
    }
    
    @Test
    void testDeleteCode() {
        // Given
        String identifier = "test@example.com";
        VerificationCodeUtils.CodeType codeType = VerificationCodeUtils.CodeType.EMAIL_REGISTER;
        
        // When
        verificationCodeUtils.deleteCode(identifier, codeType);
        
        // Then
        verify(redisTemplate).delete(anyString());
    }
}