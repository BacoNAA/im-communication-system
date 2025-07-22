package com.im.imcommunicationsystem.admin.dto.response;

import com.im.imcommunicationsystem.group.entity.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组成员响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long userId;
    private String username;
    private String avatarUrl;
    private String email;
    private String role;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private LocalDateTime joinedAt;
    
    /**
     * 从群组成员实体构建响应
     * @param member 群组成员实体
     * @param nickname 用户昵称
     * @param email 邮箱
     * @param avatarUrl 头像URL
     * @return 群组成员响应
     */
    public static GroupMemberResponse fromEntity(
            GroupMember member,
            String nickname,
            String email,
            String avatarUrl) {
        
        return GroupMemberResponse.builder()
                .userId(member.getId().getUserId())
                .username(nickname) // 前端依然使用username字段，但后端从nickname获取
                .email(email)
                .avatarUrl(avatarUrl)
                .role(member.getRole().name())
                .isMuted(member.getIsMuted())
                .mutedUntil(member.getMutedUntil())
                .joinedAt(member.getJoinedAt())
                .build();
    }
} 