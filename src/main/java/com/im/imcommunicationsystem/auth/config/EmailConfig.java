package com.im.imcommunicationsystem.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置类
 * 配置邮件发送相关参数
 */
@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailConfig {

    /**
     * 验证码有效期（分钟）
     */
    private Integer codeExpiration = 5;

    /**
     * 验证码长度
     */
    private Integer codeLength = 6;

    /**
     * 同一邮箱发送验证码间隔（秒）
     */
    private Integer sendInterval = 60;

    /**
     * 每日最大发送次数
     */
    private Integer maxSendPerDay = 10;

    /**
     * 发件人名称
     */
    private String senderName = "IM通信系统";

    /**
     * 注册验证码邮件主题
     */
    private String registrationSubject = "【IM通信系统】注册验证码";

    /**
     * 登录验证码邮件主题
     */
    private String loginSubject = "【IM通信系统】登录验证码";

    /**
     * 密码重置验证码邮件主题
     */
    private String resetPasswordSubject = "【IM通信系统】密码重置验证码";

    // Getters and Setters
    public Integer getCodeExpiration() {
        return codeExpiration;
    }

    public void setCodeExpiration(Integer codeExpiration) {
        this.codeExpiration = codeExpiration;
    }

    public Integer getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(Integer codeLength) {
        this.codeLength = codeLength;
    }

    public Integer getSendInterval() {
        return sendInterval;
    }

    public void setSendInterval(Integer sendInterval) {
        this.sendInterval = sendInterval;
    }

    public Integer getMaxSendPerDay() {
        return maxSendPerDay;
    }

    public void setMaxSendPerDay(Integer maxSendPerDay) {
        this.maxSendPerDay = maxSendPerDay;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRegistrationSubject() {
        return registrationSubject;
    }

    public void setRegistrationSubject(String registrationSubject) {
        this.registrationSubject = registrationSubject;
    }

    public String getLoginSubject() {
        return loginSubject;
    }

    public void setLoginSubject(String loginSubject) {
        this.loginSubject = loginSubject;
    }

    public String getResetPasswordSubject() {
        return resetPasswordSubject;
    }

    public void setResetPasswordSubject(String resetPasswordSubject) {
        this.resetPasswordSubject = resetPasswordSubject;
    }
}