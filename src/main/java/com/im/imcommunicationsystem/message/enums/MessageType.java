package com.im.imcommunicationsystem.message.enums;

/**
 * 消息类型枚举
 * 定义系统中支持的各种消息类型
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public enum MessageType {
    
    /**
     * 文本消息
     */
    TEXT("文本消息"),
    
    /**
     * 图片消息
     */
    IMAGE("图片消息"),
    
    /**
     * 视频消息
     */
    VIDEO("视频消息"),
    
    /**
     * 音频消息
     */
    AUDIO("音频消息"),
    
    /**
     * 文件消息
     */
    FILE("文件消息"),
    
    /**
     * 位置消息
     */
    LOCATION("位置消息"),
    
    /**
     * 表情消息
     */
    EMOJI("表情消息"),
    
    /**
     * 系统消息
     */
    SYSTEM("系统消息"),
    
    /**
     * 通知消息
     */
    NOTIFICATION("通知消息"),
    
    /**
     * 撤回消息
     */
    RECALL("撤回消息");
    
    private final String description;
    
    MessageType(String description) {
        this.description = description;
    }
    
    /**
     * 获取消息类型描述
     * 
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查是否为媒体消息
     * 
     * @return 是否为媒体消息
     */
    public boolean isMediaMessage() {
        return this == IMAGE || this == VIDEO || this == AUDIO || this == FILE;
    }
    
    /**
     * 检查是否为系统消息
     * 
     * @return 是否为系统消息
     */
    public boolean isSystemMessage() {
        return this == SYSTEM || this == NOTIFICATION;
    }
    
    /**
     * 检查是否为用户消息
     * 
     * @return 是否为用户消息
     */
    public boolean isUserMessage() {
        return !isSystemMessage() && this != RECALL;
    }
    
    /**
     * 检查是否需要存储媒体文件
     * 
     * @return 是否需要存储媒体文件
     */
    public boolean requiresMediaStorage() {
        return this == IMAGE || this == VIDEO || this == AUDIO || this == FILE;
    }
    
    /**
     * 根据字符串获取消息类型
     * 
     * @param type 类型字符串
     * @return 消息类型
     */
    public static MessageType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            return TEXT;
        }
        
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}