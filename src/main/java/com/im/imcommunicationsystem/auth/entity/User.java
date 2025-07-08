package com.im.imcommunicationsystem.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * 用户唯一标识，主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 注册邮箱，用于登录
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * 加盐哈希后的密码
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /**
     * 头像图片的URL
     */
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    /**
     * 个性签名
     */
    @Column(name = "signature", length = 255)
    private String signature;

    /**
     * 用户设置的唯一ID，可用于搜索
     */
    @Column(name = "user_id_str", unique = true, length = 50)
    private String userIdStr;

    /**
     * 个性化状态 (如 {"emoji": "💻", "text": "工作中", "expires_at": "..."})
     */
    @Column(name = "status_json", columnDefinition = "JSON")
    private String statusJson;

    /**
     * 账户创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 资料更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}