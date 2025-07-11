package com.im.imcommunicationsystem.relationship.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 好友标签实体类
 * 对应数据库表：contact_tags
 * 管理用户的好友标签分类
 */
@Entity
@Table(name = "contact_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactTag {

    /**
     * 标签ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标签所有者ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 标签名称
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 标签颜色（十六进制颜色代码）
     */
    @Column(name = "color", length = 7)
    private String color;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}