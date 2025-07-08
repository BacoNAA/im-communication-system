package com.im.imcommunicationsystem.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 验证码配置类
 * 外部化验证码相关配置，提高可维护性
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.verification-code")
@Validated
public class VerificationCodeConfig {

    /**
     * 默认验证码长度
     */
    @Min(value = 4, message = "验证码长度不能少于4位")
    @Max(value = 8, message = "验证码长度不能超过8位")
    private int defaultLength = 6;

    /**
     * 默认过期时间（分钟）
     */
    @Positive(message = "过期时间必须为正数")
    @Max(value = 60, message = "过期时间不能超过60分钟")
    private int defaultExpireMinutes = 5;

    /**
     * 发送间隔时间（秒）
     */
    @Positive(message = "发送间隔必须为正数")
    @Max(value = 300, message = "发送间隔不能超过5分钟")
    private int sendIntervalSeconds = 60;

    /**
     * 最大重试次数
     */
    @Min(value = 3, message = "最大重试次数不能少于3次")
    @Max(value = 10, message = "最大重试次数不能超过10次")
    private int maxRetryCount = 5;

    /**
     * 验证码类型配置
     */
    @NotNull(message = "验证码类型配置不能为空")
    private CodeTypeConfig codeTypes = new CodeTypeConfig();

    /**
     * 验证码类型配置
     */
    @Data
    public static class CodeTypeConfig {
        
        /**
         * 邮箱注册验证码配置
         */
        private CodeConfig emailRegister = new CodeConfig(6, 5, "EMAIL_REG");
        
        /**
         * 邮箱登录验证码配置
         */
        private CodeConfig emailLogin = new CodeConfig(6, 5, "EMAIL_LOGIN");
        
        /**
         * 密码重置验证码配置
         */
        private CodeConfig passwordReset = new CodeConfig(6, 10, "PWD_RESET");
        
        /**
         * 邮箱绑定验证码配置
         */
        private CodeConfig emailBind = new CodeConfig(6, 5, "EMAIL_BIND");
        
        /**
         * 手机验证码配置
         */
        private CodeConfig phoneVerify = new CodeConfig(6, 5, "PHONE_VERIFY");
        
        /**
         * 图形验证码配置
         */
        private CodeConfig captcha = new CodeConfig(4, 2, "CAPTCHA");
        
        /**
         * 二次验证码配置
         */
        private CodeConfig twoFactor = new CodeConfig(6, 10, "TWO_FACTOR");
    }

    /**
     * 单个验证码配置
     */
    @Data
    public static class CodeConfig {
        
        /**
         * 验证码长度
         */
        @Min(value = 4, message = "验证码长度不能少于4位")
        @Max(value = 8, message = "验证码长度不能超过8位")
        private int length;
        
        /**
         * 过期时间（分钟）
         */
        @Positive(message = "过期时间必须为正数")
        private int expireMinutes;
        
        /**
         * Redis键前缀
         */
        private String prefix;
        
        public CodeConfig() {}
        
        public CodeConfig(int length, int expireMinutes, String prefix) {
            this.length = length;
            this.expireMinutes = expireMinutes;
            this.prefix = prefix;
        }
    }

    /**
     * 安全配置
     */
    @Data
    public static class SecurityConfig {
        
        /**
         * 是否启用IP限制
         */
        private boolean enableIpLimit = true;
        
        /**
         * 单个IP每小时最大发送次数
         */
        @Positive(message = "IP限制次数必须为正数")
        private int maxSendPerHour = 10;
        
        /**
         * 是否启用验证码复杂度检查
         */
        private boolean enableComplexityCheck = false;
    }

    /**
     * 安全配置
     */
    @NotNull(message = "安全配置不能为空")
    private SecurityConfig security = new SecurityConfig();
}