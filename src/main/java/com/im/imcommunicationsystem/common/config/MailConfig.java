package com.im.imcommunicationsystem.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置类
 * 使用Spring Boot自动配置的JavaMailSender
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class MailConfig {
    // Spring Boot会根据application.yml中的spring.mail配置自动创建JavaMailSender bean
    // 移除自定义配置以避免冲突
}