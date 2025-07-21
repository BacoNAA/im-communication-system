package com.im.imcommunicationsystem.moment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 动态模块配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moment")
public class MomentConfig {
    
    /**
     * 动态内容最大长度（2000字）
     */
    private int maxContentLength = 2000;
    
    /**
     * 评论内容最大长度（1000字）
     */
    private int maxCommentLength = 1000;
    
    /**
     * 时间线分页大小
     */
    private int timelinePageSize = 10;
    
    /**
     * 评论分页大小
     */
    private int commentPageSize = 20;
    
    /**
     * 缓存过期时间（秒）
     */
    private int cacheTtl = 300;
    
    /**
     * 是否启用通知
     */
    private boolean enableNotification = true;
    
    /**
     * 动态默认可见性
     */
    private String defaultVisibility = "PUBLIC";
    
    /**
     * 是否允许删除已有评论的动态
     */
    private boolean allowDeleteWithComments = true;
    
    /**
     * 媒体配置
     */
    private final MediaConfig media = new MediaConfig();
} 