package com.im.imcommunicationsystem.message.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 媒体文件类型枚举
 * 定义系统中支持的媒体文件类型
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public enum MediaType {
    
    /**
     * 图片
     */
    IMAGE("图片", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"), 
          Arrays.asList("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml"),
          10 * 1024 * 1024), // 10MB
    
    /**
     * 视频
     */
    VIDEO("视频", Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v"), 
          Arrays.asList("video/mp4", "video/avi", "video/quicktime", "video/x-ms-wmv", "video/x-flv", "video/x-matroska", "video/webm"),
          100 * 1024 * 1024), // 100MB
    
    /**
     * 音频
     */
    AUDIO("音频", Arrays.asList("mp3", "wav", "aac", "ogg", "flac", "m4a", "wma"), 
          Arrays.asList("audio/mpeg", "audio/wav", "audio/aac", "audio/ogg", "audio/flac", "audio/mp4", "audio/x-ms-wma"),
          50 * 1024 * 1024), // 50MB
    
    /**
     * 文档
     */
    DOCUMENT("文档", Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf"), 
             Arrays.asList("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                          "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                          "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                          "text/plain", "application/rtf"),
             20 * 1024 * 1024), // 20MB
    
    /**
     * 其他文件
     */
    OTHER("其他", Arrays.asList(), Arrays.asList(), 50 * 1024 * 1024); // 50MB
    
    private final String description;
    private final List<String> supportedExtensions;
    private final List<String> supportedMimeTypes;
    private final long maxFileSize;
    
    MediaType(String description, List<String> supportedExtensions, List<String> supportedMimeTypes, long maxFileSize) {
        this.description = description;
        this.supportedExtensions = supportedExtensions;
        this.supportedMimeTypes = supportedMimeTypes;
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * 获取媒体类型描述
     * 
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取支持的文件扩展名
     * 
     * @return 支持的扩展名列表
     */
    public List<String> getSupportedExtensions() {
        return supportedExtensions;
    }
    
    /**
     * 获取支持的MIME类型
     * 
     * @return 支持的MIME类型列表
     */
    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }
    
    /**
     * 获取最大文件大小
     * 
     * @return 最大文件大小（字节）
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * 检查是否支持指定的文件扩展名
     * 
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public boolean supportsExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }
        
        String normalizedExtension = extension.toLowerCase().replaceAll("^\\.", "");
        return supportedExtensions.contains(normalizedExtension);
    }
    
    /**
     * 检查是否支持指定的MIME类型
     * 
     * @param mimeType MIME类型
     * @return 是否支持
     */
    public boolean supportsMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return false;
        }
        
        return supportedMimeTypes.contains(mimeType.toLowerCase());
    }
    
    /**
     * 检查文件大小是否在允许范围内
     * 
     * @param fileSize 文件大小
     * @return 是否在允许范围内
     */
    public boolean isValidFileSize(long fileSize) {
        return fileSize > 0 && fileSize <= maxFileSize;
    }
    
    /**
     * 检查是否需要生成缩略图
     * 
     * @return 是否需要生成缩略图
     */
    public boolean needsThumbnail() {
        return this == IMAGE || this == VIDEO;
    }
    
    /**
     * 检查是否可以在线预览
     * 
     * @return 是否可以在线预览
     */
    public boolean canPreview() {
        return this == IMAGE || this == DOCUMENT;
    }
    
    /**
     * 检查是否可以在线播放
     * 
     * @return 是否可以在线播放
     */
    public boolean canPlay() {
        return this == VIDEO || this == AUDIO;
    }
    
    /**
     * 根据文件扩展名获取媒体类型
     * 
     * @param extension 文件扩展名
     * @return 媒体类型
     */
    public static MediaType fromExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return OTHER;
        }
        
        String normalizedExtension = extension.toLowerCase().replaceAll("^\\.", "");
        
        for (MediaType type : values()) {
            if (type.supportsExtension(normalizedExtension)) {
                return type;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 根据MIME类型获取媒体类型
     * 
     * @param mimeType MIME类型
     * @return 媒体类型
     */
    public static MediaType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return OTHER;
        }
        
        String normalizedMimeType = mimeType.toLowerCase();
        
        for (MediaType type : values()) {
            if (type.supportsMimeType(normalizedMimeType)) {
                return type;
            }
        }
        
        return OTHER;
    }
    
    /**
     * 根据文件名获取媒体类型
     * 
     * @param fileName 文件名
     * @return 媒体类型
     */
    public static MediaType fromFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return OTHER;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return OTHER;
        }
        
        String extension = fileName.substring(lastDotIndex + 1);
        return fromExtension(extension);
    }
    
    /**
     * 根据字符串获取媒体类型
     * 
     * @param type 类型字符串
     * @return 媒体类型
     */
    public static MediaType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            return OTHER;
        }
        
        try {
            return MediaType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
    
    /**
     * 获取可读的文件大小限制
     * 
     * @return 可读的文件大小限制
     */
    public String getReadableMaxFileSize() {
        long size = maxFileSize;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%d %s", size, units[unitIndex]);
    }
}