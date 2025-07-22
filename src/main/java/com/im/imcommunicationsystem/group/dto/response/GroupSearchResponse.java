package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组搜索响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchResponse {
    
    /**
     * 群组ID
     */
    private Long id;
    
    /**
     * 群组名称
     */
    private String name;
    
    /**
     * 群组头像URL
     */
    private String avatarUrl;
    
    /**
     * 群组描述
     */
    private String description;
    
    /**
     * 群主ID
     */
    private Long ownerId;
    
    /**
     * 群主用户名
     */
    private String ownerName;
    
    /**
     * 成员数量
     */
    private Integer memberCount;
    
    /**
     * 是否需要审批
     */
    private Boolean requiresApproval;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 当前用户是否已是该群成员
     */
    private Boolean isMember;
    
    /**
     * 当前用户是否有待处理的加入请求
     */
    private Boolean hasPendingRequest;
    
    /**
     * 群组是否被封禁
     */
    private Boolean isBanned;
    
    /**
     * 封禁原因
     */
    private String bannedReason;
    
    /**
     * 封禁截止时间
     */
    private LocalDateTime bannedUntil;
} 