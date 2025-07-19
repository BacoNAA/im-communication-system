package com.im.imcommunicationsystem.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户设置实体类
 * 对应数据库表：user_settings
 */
@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    /**
     * 关联用户ID，主键
     */
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 隐私设置（JSON格式）
     */
    @Column(name = "privacy_settings", columnDefinition = "JSON")
    private String privacySettings;

    /**
     * 通知设置（JSON格式）
     */
    @Column(name = "notification_settings", columnDefinition = "JSON")
    private String notificationSettings;

    /**
 * 主题设置（JSON格式）
 * 包含：
 * - color: 主题颜色，如 "#1890ff"
 * - chatBackground: 聊天背景，可以是颜色值或图片URL
 * - fontSize: 字体大小，默认为14
 */
@Column(name = "theme_settings", columnDefinition = "JSON")
private String themeSettings;

    /**
     * 语言设置
     */
    @Column(name = "language")
    private String language = "zh-CN";

    /**
     * 设置更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}