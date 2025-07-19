package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群成员响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatarUrl;
    
    /**
     * 群昵称
     */
    private String groupNickname;
    
    /**
     * 角色
     */
    private String role;
    
    /**
     * 是否被禁言
     */
    private Boolean isMuted;
    
    /**
     * 禁言截止时间
     */
    private LocalDateTime mutedUntil;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
    
    /**
     * 在线状态
     */
    private Boolean isOnline;
} 