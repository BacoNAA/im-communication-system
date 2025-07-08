package com.im.imcommunicationsystem.auth.entity;

import com.im.imcommunicationsystem.auth.enums.VerificationCodeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 验证码实体类
 * 对应数据库表：verification_codes（缓存表）
 */
@Entity
@Table(name = "verification_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCode {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邮箱地址
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * 验证码
     */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /**
     * 验证码类型（注册/登录/重置密码）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VerificationCodeType codeType;

    /**
     * 是否已使用
     */
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    /**
     * 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}