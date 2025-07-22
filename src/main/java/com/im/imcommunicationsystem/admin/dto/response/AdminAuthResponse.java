package com.im.imcommunicationsystem.admin.dto.response;

import com.im.imcommunicationsystem.admin.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员认证响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 管理员信息
     */
    private AdminInfo adminInfo;

    /**
     * 管理员信息DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {

        /**
         * 管理员ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 角色
         */
        private String role;

        /**
         * 权限
         */
        private String permissions;

        /**
         * 最后登录时间
         */
        private LocalDateTime lastLoginAt;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;

        /**
         * 是否激活
         */
        private Boolean isActive;

        /**
         * 从实体转换为DTO
         * @param adminUser 管理员用户实体
         * @return 管理员信息DTO
         */
        public static AdminInfo fromEntity(AdminUser adminUser) {
            return AdminInfo.builder()
                    .id(adminUser.getId())
                    .username(adminUser.getUsername())
                    .email(adminUser.getEmail())
                    .role(adminUser.getRole().name())  // 直接使用枚举的name()，现在枚举名称与数据库值匹配
                    .permissions(adminUser.getPermissions())
                    .lastLoginAt(adminUser.getLastLoginAt())
                    .createdAt(adminUser.getCreatedAt())
                    .isActive(adminUser.getIsActive())
                    .build();
        }
    }
} 