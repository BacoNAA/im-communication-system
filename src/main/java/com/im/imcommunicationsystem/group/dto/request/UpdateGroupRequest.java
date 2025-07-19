package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新群组请求DTO
 * 用于接收更新群组的请求参数
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    /**
     * 群名称
     */
    @Size(min = 1, max = 100, message = "群名称长度必须在1-100之间")
    private String name;

    /**
     * 群介绍
     */
    @Size(max = 500, message = "群介绍长度不能超过500")
    private String description;

    /**
     * 群头像URL
     */
    @Size(max = 500, message = "群头像URL长度不能超过500")
    private String avatarUrl;

    /**
     * 最大成员数
     */
    private Integer maxMembers;

    /**
     * 是否需要审批
     */
    private Boolean requiresApproval;
} 