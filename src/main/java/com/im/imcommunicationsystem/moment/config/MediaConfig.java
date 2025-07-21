package com.im.imcommunicationsystem.moment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 媒体配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moment.media")
public class MediaConfig {
    
    /**
     * 图片文件最大大小（10MB）
     */
    private long maxImageSize = 10 * 1024 * 1024;
    
    /**
     * 视频文件最大大小（100MB）
     */
    private long maxVideoSize = 100 * 1024 * 1024;
    
    /**
     * 单条动态最大图片数量（9张）
     */
    private int maxImageCount = 9;
    
    /**
     * 支持的图片格式
     */
    private List<String> supportedImageFormats = Arrays.asList("image/jpeg", "image/png", "image/gif");
    
    /**
     * 支持的视频格式
     */
    private List<String> supportedVideoFormats = Arrays.asList("video/mp4", "video/mpeg", "video/quicktime");
    
    /**
     * 视频最大时长（秒）
     */
    private int videoMaxDuration = 60;
    
    /**
     * 图片压缩质量（0-1）
     */
    private float imageCompressionQuality = 0.8f;
    
    /**
     * 上传目录
     */
    private String uploadDirectory = "uploads/moments";
    
    /**
     * 是否启用图片水印
     */
    private boolean enableWatermark = false;
    
    /**
     * 水印文本
     */
    private String watermarkText = "";
} 