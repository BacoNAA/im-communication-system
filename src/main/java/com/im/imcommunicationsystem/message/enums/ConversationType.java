package com.im.imcommunicationsystem.message.enums;

/**
 * 会话类型枚举
 * 定义系统中支持的会话类型
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public enum ConversationType {
    
    /**
     * 私聊
     */
    PRIVATE("私聊", 2),
    
    /**
     * 群聊
     */
    GROUP("群聊", 500),
    
    /**
     * 系统通知
     */
    SYSTEM("系统通知", 1),
    
    /**
     * 频道
     */
    CHANNEL("频道", 10000),
    
    /**
     * 临时会话
     */
    TEMPORARY("临时会话", 2);
    
    private final String description;
    private final int maxParticipants;
    
    ConversationType(String description, int maxParticipants) {
        this.description = description;
        this.maxParticipants = maxParticipants;
    }
    
    /**
     * 获取会话类型描述
     * 
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取最大参与者数量
     * 
     * @return 最大参与者数量
     */
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    /**
     * 检查是否为私聊
     * 
     * @return 是否为私聊
     */
    public boolean isPrivate() {
        return this == PRIVATE || this == TEMPORARY;
    }
    
    /**
     * 检查是否为群聊
     * 
     * @return 是否为群聊
     */
    public boolean isGroup() {
        return this == GROUP || this == CHANNEL;
    }
    
    /**
     * 检查是否为系统会话
     * 
     * @return 是否为系统会话
     */
    public boolean isSystem() {
        return this == SYSTEM;
    }
    
    /**
     * 检查是否为临时会话
     * 
     * @return 是否为临时会话
     */
    public boolean isTemporary() {
        return this == TEMPORARY;
    }
    
    /**
     * 检查是否支持多人参与
     * 
     * @return 是否支持多人参与
     */
    public boolean supportsMultipleParticipants() {
        return maxParticipants > 2;
    }
    
    /**
     * 检查是否需要管理员权限
     * 
     * @return 是否需要管理员权限
     */
    public boolean requiresAdminPermission() {
        return this == GROUP || this == CHANNEL;
    }
    
    /**
     * 检查是否支持邀请功能
     * 
     * @return 是否支持邀请功能
     */
    public boolean supportsInvitation() {
        return this == GROUP || this == CHANNEL;
    }
    
    /**
     * 检查是否支持踢出功能
     * 
     * @return 是否支持踢出功能
     */
    public boolean supportsKickOut() {
        return this == GROUP || this == CHANNEL;
    }
    
    /**
     * 检查是否支持角色管理
     * 
     * @return 是否支持角色管理
     */
    public boolean supportsRoleManagement() {
        return this == GROUP || this == CHANNEL;
    }
    
    /**
     * 检查参与者数量是否有效
     * 
     * @param participantCount 参与者数量
     * @return 是否有效
     */
    public boolean isValidParticipantCount(int participantCount) {
        if (participantCount <= 0) {
            return false;
        }
        
        switch (this) {
            case PRIVATE:
            case TEMPORARY:
                return participantCount == 2;
            case SYSTEM:
                return participantCount == 1;
            case GROUP:
            case CHANNEL:
                return participantCount <= maxParticipants;
            default:
                return false;
        }
    }
    
    /**
     * 根据字符串获取会话类型
     * 
     * @param type 类型字符串
     * @return 会话类型
     */
    public static ConversationType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            return PRIVATE;
        }
        
        try {
            return ConversationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PRIVATE;
        }
    }
}