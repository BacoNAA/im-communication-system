package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    
    /**
     * 群组ID
     */
    private Long id;
    
    /**
     * 群组名称
     */
    private String name;
    
    /**
     * 群组描述
     */
    private String description;
    
    /**
     * 群头像URL
     */
    private String avatarUrl;
    
    /**
     * 群主ID
     */
    private Long ownerId;
    
    /**
     * 群主昵称
     */
    private String ownerNickname;
    
    /**
     * 成员数量
     */
    private Integer memberCount;
    
    /**
     * 是否需要审批
     */
    private Boolean requiresApproval;
    
    /**
     * 是否全体禁言
     */
    private Boolean isAllMuted;
    
    /**
     * 当前用户角色
     */
    private String userRole;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
} 