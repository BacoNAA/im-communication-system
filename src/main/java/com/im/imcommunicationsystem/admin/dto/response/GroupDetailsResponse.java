package com.im.imcommunicationsystem.admin.dto.response;

import com.im.imcommunicationsystem.group.entity.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailsResponse {
    private Long groupId;
    private String name;
    private String description;
    private String avatarUrl;
    private Long ownerId;
    private String ownerUsername;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private Boolean requiresApproval;
    private Boolean isAllMuted;
    
    // 封禁相关字段
    private Boolean isBanned;
    private String bannedReason;
    private LocalDateTime bannedUntil;

    /**
     * 从群组实体构建响应
     * @param group 群组实体
     * @param memberCount 成员数量
     * @param ownerNickname 群主昵称
     * @return 群组详情响应
     */
    public static GroupDetailsResponse fromEntity(Group group, Integer memberCount, String ownerNickname) {
        return GroupDetailsResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .ownerId(group.getOwnerId())
                .ownerUsername(ownerNickname) // 前端依然使用ownerUsername字段，但后端从ownerNickname获取
                .memberCount(memberCount)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .requiresApproval(group.getRequiresApproval())
                .isAllMuted(group.getIsAllMuted())
                .isBanned(group.getIsBanned())
                .bannedReason(group.getBannedReason())
                .bannedUntil(group.getBannedUntil())
                .status(group.getIsBanned() ? "banned" : "active")
                .build();
    }
} 