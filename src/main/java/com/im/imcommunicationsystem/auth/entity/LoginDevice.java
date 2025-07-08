package com.im.imcommunicationsystem.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 登录设备实体类
 * 对应数据库表：login_devices
 */
@Entity
@Table(name = "login_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDevice {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 设备类型 (如 Android, Web)
     */
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    /**
     * 设备信息 (如 Chrome 125.0)
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * 登录IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 最近一次登录时间
     */
    @CreationTimestamp
    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    /**
     * 会话是否依然有效
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}