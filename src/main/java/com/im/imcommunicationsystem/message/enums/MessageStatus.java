package com.im.imcommunicationsystem.message.enums;

/**
 * 消息状态枚举
 * 定义消息的各种状态
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public enum MessageStatus {
    
    /**
     * 发送中
     */
    SENDING("发送中"),
    
    /**
     * 已发送
     */
    SENT("已发送"),
    
    /**
     * 已送达
     */
    DELIVERED("已送达"),
    
    /**
     * 已读
     */
    READ("已读"),
    
    /**
     * 发送失败
     */
    FAILED("发送失败"),
    
    /**
     * 已撤回
     */
    RECALLED("已撤回"),
    
    /**
     * 已删除
     */
    DELETED("已删除");
    
    private final String description;
    
    MessageStatus(String description) {
        this.description = description;
    }
    
    /**
     * 获取状态描述
     * 
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查是否为最终状态
     * 
     * @return 是否为最终状态
     */
    public boolean isFinalStatus() {
        return this == READ || this == FAILED || this == RECALLED || this == DELETED;
    }
    
    /**
     * 检查是否为成功状态
     * 
     * @return 是否为成功状态
     */
    public boolean isSuccessStatus() {
        return this == SENT || this == DELIVERED || this == READ;
    }
    
    /**
     * 检查是否为失败状态
     * 
     * @return 是否为失败状态
     */
    public boolean isFailureStatus() {
        return this == FAILED;
    }
    
    /**
     * 检查是否为进行中状态
     * 
     * @return 是否为进行中状态
     */
    public boolean isInProgressStatus() {
        return this == SENDING;
    }
    
    /**
     * 检查是否可以撤回
     * 
     * @return 是否可以撤回
     */
    public boolean canRecall() {
        return this == SENT || this == DELIVERED || this == READ;
    }
    
    /**
     * 检查是否可以删除
     * 
     * @return 是否可以删除
     */
    public boolean canDelete() {
        return this != DELETED;
    }
    
    /**
     * 检查是否可以编辑
     * 
     * @return 是否可以编辑
     */
    public boolean canEdit() {
        return this == SENT || this == DELIVERED || this == READ;
    }
    
    /**
     * 获取下一个状态
     * 
     * @return 下一个状态
     */
    public MessageStatus getNextStatus() {
        switch (this) {
            case SENDING:
                return SENT;
            case SENT:
                return DELIVERED;
            case DELIVERED:
                return READ;
            default:
                return this;
        }
    }
    
    /**
     * 根据字符串获取消息状态
     * 
     * @param status 状态字符串
     * @return 消息状态
     */
    public static MessageStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return SENDING;
        }
        
        try {
            return MessageStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SENDING;
        }
    }
}