package com.im.imcommunicationsystem.common.service;

/**
 * 邮件服务接口
 * 提供邮件发送相关功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface EmailService {
    
    /**
     * 发送验证码邮件
     * 
     * @param to 收件人邮箱
     * @param code 验证码
     * @param subject 邮件主题
     */
    void sendVerificationCode(String to, String code, String subject);
    
    /**
     * 发送普通邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);
}