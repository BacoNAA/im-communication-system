package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入群组请求DTO
 * 用于接收加入群组的请求参数
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinGroupRequest {

    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    @Size(min = 1, max = 50, message = "邀请码长度必须在1-50之间")
    private String inviteCode;

    /**
     * 申请消息
     */
    @Size(max = 200, message = "申请消息长度不能超过200")
    private String message;
} 