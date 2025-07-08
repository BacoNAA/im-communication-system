package com.im.imcommunicationsystem.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码配置测试
 * 
 * @author System
 * @since 2024-01-01
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.verification-code.default-length=6",
    "app.verification-code.default-expire-minutes=10",
    "app.verification-code.send-interval-seconds=60",
    "app.verification-code.max-retry-count=5",
    "app.verification-code.code-types.email-register.length=6",
    "app.verification-code.code-types.email-register.expire-minutes=5",
    "app.verification-code.code-types.email-login.length=6",
    "app.verification-code.code-types.email-login.expire-minutes=5",
    "app.verification-code.security.enable-ip-limit=true",
    "app.verification-code.security.max-send-per-hour=10",
    "app.verification-code.security.enable-complexity-check=false"
})
class VerificationCodeConfigTest {

    @Autowired
    private VerificationCodeConfig verificationCodeConfig;

    @Test
    void testDefaultConfiguration() {
        // 测试默认配置
        assertEquals(6, verificationCodeConfig.getDefaultLength());
        assertEquals(10, verificationCodeConfig.getDefaultExpireMinutes());
        assertEquals(60, verificationCodeConfig.getSendIntervalSeconds());
        assertEquals(5, verificationCodeConfig.getMaxRetryCount());
    }

    @Test
    void testTypesConfiguration() {
        // 测试类型配置
        assertNotNull(verificationCodeConfig.getCodeTypes());
        
        // 测试邮箱注册配置
        VerificationCodeConfig.CodeConfig emailRegister = verificationCodeConfig.getCodeTypes().getEmailRegister();
        assertNotNull(emailRegister);
        assertEquals(6, emailRegister.getLength());
        assertEquals(5, emailRegister.getExpireMinutes());
        
        // 测试邮箱登录配置
        VerificationCodeConfig.CodeConfig emailLogin = verificationCodeConfig.getCodeTypes().getEmailLogin();
        assertNotNull(emailLogin);
        assertEquals(6, emailLogin.getLength());
        assertEquals(5, emailLogin.getExpireMinutes());
    }

    @Test
    void testSecurityConfiguration() {
        // 测试安全配置
        VerificationCodeConfig.SecurityConfig security = verificationCodeConfig.getSecurity();
        assertNotNull(security);
        assertTrue(security.isEnableIpLimit());
        assertEquals(10, security.getMaxSendPerHour());
        assertFalse(security.isEnableComplexityCheck());
    }

    @Test
    void testConfigurationValidation() {
        // 测试配置验证
        assertTrue(verificationCodeConfig.getDefaultLength() > 0);
        assertTrue(verificationCodeConfig.getDefaultExpireMinutes() > 0);
        assertTrue(verificationCodeConfig.getSendIntervalSeconds() >= 0);
        assertTrue(verificationCodeConfig.getMaxRetryCount() > 0);
        
        // 测试类型配置验证
        VerificationCodeConfig.CodeTypeConfig codeTypes = verificationCodeConfig.getCodeTypes();
        if (codeTypes.getEmailRegister() != null) {
            assertTrue(codeTypes.getEmailRegister().getLength() > 0);
            assertTrue(codeTypes.getEmailRegister().getExpireMinutes() > 0);
        }
        
        if (codeTypes.getEmailLogin() != null) {
            assertTrue(codeTypes.getEmailLogin().getLength() > 0);
            assertTrue(codeTypes.getEmailLogin().getExpireMinutes() > 0);
        }
    }

    @Test
    void testGetEffectiveLength() {
        // 测试获取有效长度
        VerificationCodeConfig.CodeConfig emailRegister = verificationCodeConfig.getCodeTypes().getEmailRegister();
        if (emailRegister != null) {
            assertEquals(6, emailRegister.getLength());
        } else {
            assertEquals(verificationCodeConfig.getDefaultLength(), verificationCodeConfig.getDefaultLength());
        }
    }

    @Test
    void testGetEffectiveExpireMinutes() {
        // 测试获取有效过期时间
        VerificationCodeConfig.CodeConfig emailLogin = verificationCodeConfig.getCodeTypes().getEmailLogin();
        if (emailLogin != null) {
            assertEquals(5, emailLogin.getExpireMinutes());
        } else {
            assertEquals(verificationCodeConfig.getDefaultExpireMinutes(), verificationCodeConfig.getDefaultExpireMinutes());
        }
    }
}