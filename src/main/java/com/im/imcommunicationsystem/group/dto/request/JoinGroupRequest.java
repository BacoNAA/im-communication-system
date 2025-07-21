package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入群组请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinGroupRequest {
    
    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;
    
    /**
     * 申请消息
     */
    private String message;
} 