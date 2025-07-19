package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群邀请响应DTO
 * 用于返回群邀请信息
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInviteResponse {

    /**
     * 邀请ID
     */
    private Long id;

    /**
     * 群组ID
     */
    private Long groupId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 邀请者ID
     */
    private Long inviterId;

    /**
     * 邀请者名称
     */
    private String inviterName;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 邀请链接
     */
    private String inviteLink;

    /**
     * 最大使用次数
     */
    private Integer maxUses;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 是否已禁用
     */
    private Boolean isDisabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
} 