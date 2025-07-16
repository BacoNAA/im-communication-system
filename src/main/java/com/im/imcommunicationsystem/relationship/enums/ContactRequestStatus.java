package com.im.imcommunicationsystem.relationship.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 好友请求状态枚举
 * 对应数据库中friend_requests表的status字段
 */
public enum ContactRequestStatus {
    
    /**
     * 待处理
     */
    PENDING("pending", "待处理"),
    
    /**
     * 已接受
     */
    ACCEPTED("accepted", "已接受"),
    
    /**
     * 已拒绝
     */
    REJECTED("rejected", "已拒绝");
    
    private final String code;
    private final String description;
    
    ContactRequestStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举值
     * @param code 状态代码
     * @return 对应的枚举值
     */
    public static ContactRequestStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        // 处理小写输入
        String normalizedCode = code.toLowerCase();
        
        for (ContactRequestStatus status : values()) {
            if (status.code.toLowerCase().equals(normalizedCode)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contact request status code: " + code);
    }
}