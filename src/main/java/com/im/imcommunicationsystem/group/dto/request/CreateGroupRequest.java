package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建群组请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    
    /**
     * 群组名称
     */
    @NotBlank(message = "群组名称不能为空")
    @Size(min = 1, max = 50, message = "群组名称长度必须在1-50个字符之间")
    private String name;
    
    /**
     * 群组描述
     */
    @Size(max = 500, message = "群组描述长度不能超过500个字符")
    private String description;
    
    /**
     * 群头像URL
     */
    private String avatarUrl;
    
    /**
     * 是否需要审批
     */
    private Boolean requiresApproval;
    
    /**
     * 初始成员ID列表
     */
    @NotNull(message = "成员列表不能为空")
    @Size(min = 1, message = "至少需要一个成员")
    private List<Long> memberIds;
} 