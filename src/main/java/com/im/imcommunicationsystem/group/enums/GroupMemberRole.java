package com.im.imcommunicationsystem.group.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 群成员角色枚举
 */
public enum GroupMemberRole {
    owner,  // 群主
    admin,  // 管理员
    member;  // 普通成员
    
    @JsonCreator
    public static GroupMemberRole fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (GroupMemberRole role : GroupMemberRole.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        
        throw new IllegalArgumentException("Unknown GroupMemberRole: " + value);
    }
} 