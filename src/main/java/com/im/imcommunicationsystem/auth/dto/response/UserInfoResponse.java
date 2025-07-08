package com.im.imcommunicationsystem.auth.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 密码（用于记住登录状态）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 用户唯一ID字符串
     */
    private String userIdStr;

    /**
     * 个性化状态（JSON格式）
     */
    private String personalizedStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}