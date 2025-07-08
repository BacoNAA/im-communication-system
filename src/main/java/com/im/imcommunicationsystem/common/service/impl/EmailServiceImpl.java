package com.im.imcommunicationsystem.common.service.impl;

import com.im.imcommunicationsystem.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 * 目前使用控制台输出模拟邮件发送，便于开发测试
 * 生产环境需要配置真实的邮件服务
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    // 发件人邮箱地址，从配置文件中读取
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Override
    public void sendVerificationCode(String to, String code, String subject) {
        log.info("开始发送验证码邮件到: {}", to);
        
        // 验证邮箱格式
        if (!isValidEmailFormat(to)) {
            log.error("邮箱格式不正确: {}", to);
            throw new RuntimeException("邮箱格式不正确: " + to);
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            
            // 构建更专业的邮件内容
            String emailContent = buildVerificationEmailContent(code);
            message.setText(emailContent);
            
            // 发送邮件
            mailSender.send(message);
            
            // 验证邮件是否成功发送（通过检查是否抛出异常）
            log.info("验证码邮件发送成功: {}, 验证码: {}", to, code);
            
            // 可以在这里添加额外的发送状态检查逻辑
            // 例如：检查邮件服务器响应、记录发送日志等
            
        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.error("邮件认证失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件服务认证失败，请联系管理员", e);
        } catch (org.springframework.mail.MailSendException e) {
            log.error("邮件发送失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败，请检查邮箱地址是否正确", e);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("开始发送邮件到: {}, 主题: {}", to, subject);
        
        // 验证邮箱格式
        if (!isValidEmailFormat(to)) {
            log.error("邮箱格式不正确: {}", to);
            throw new RuntimeException("邮箱格式不正确: " + to);
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("邮件发送成功: {}", to);
            
        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.error("邮件认证失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件服务认证失败，请联系管理员", e);
        } catch (org.springframework.mail.MailSendException e) {
            log.error("邮件发送失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败，请检查邮箱地址是否正确", e);
        } catch (Exception e) {
            log.error("邮件发送失败: {}, 错误: {}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱地址
     * @return 是否有效
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // 基本的邮箱格式验证
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * 构建验证码邮件内容
     * 
     * @param code 验证码
     * @return 邮件内容
     */
    private String buildVerificationEmailContent(String code) {
        StringBuilder content = new StringBuilder();
        content.append("尊敬的用户，您好！\n\n");
        content.append("您正在进行邮箱验证，验证码为：").append(code).append("\n\n");
        content.append("请在5分钟内使用此验证码完成验证。\n");
        content.append("如果这不是您本人的操作，请忽略此邮件。\n\n");
        content.append("为了您的账户安全，请勿将验证码告知他人。\n\n");
        content.append("此邮件由系统自动发送，请勿回复。\n\n");
        content.append("感谢您的使用！\n");
        content.append("IM通信系统团队");
        
        return content.toString();
    }
}