package com.im.imcommunicationsystem.admin.enums;

/**
 * 管理员操作目标类型枚举
 */
public enum TargetType {
    /**
     * 管理员
     */
    ADMIN("管理员"),
    
    /**
     * 用户
     */
    USER("用户"),
    
    /**
     * 群组
     */
    GROUP("群组"),
    
    /**
     * 消息
     */
    MESSAGE("消息"),
    
    /**
     * 动态
     */
    MOMENT("动态"),
    
    /**
     * 评论
     */
    COMMENT("评论"),
    
    /**
     * 举报
     */
    REPORT("举报"),
    
    /**
     * 系统配置
     */
    SYSTEM_CONFIG("系统配置"),
    
    /**
     * 系统日志
     */
    SYSTEM_LOG("系统日志"),
    
    /**
     * 系统通知
     */
    NOTIFICATION("系统通知"),
    
    /**
     * 其他
     */
    OTHER("其他");
    
    /**
     * 目标类型描述
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param description 目标类型描述
     */
    TargetType(String description) {
        this.description = description;
    }
    
    /**
     * 获取目标类型描述
     * 
     * @return 目标类型描述
     */
    public String getDescription() {
        return this.description;
    }
} 