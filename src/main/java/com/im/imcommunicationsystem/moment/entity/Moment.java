package com.im.imcommunicationsystem.moment.entity;

import com.im.imcommunicationsystem.moment.enums.MediaType;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 动态实体类
 */
@Data
@Entity
@Table(name = "moments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Moment {

    /**
     * 动态唯一标识
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 发布用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 动态文字内容
     */
    @Column(name = "content", length = 2000)
    private String content;

    /**
     * 媒体文件URL列表（JSON格式）
     */
    @Column(name = "media_urls", columnDefinition = "json")
    private String mediaUrls;

    /**
     * 媒体类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    /**
     * 可见性类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_type", nullable = false)
    private VisibilityType visibilityType;

    /**
     * 可见性规则（JSON格式）
     */
    @Column(name = "visibility_rules", columnDefinition = "json")
    private String visibilityRules;

    /**
     * 点赞数量
     */
    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    /**
     * 评论数量
     */
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 动态点赞列表（不参与数据库映射）
     */
    @Transient
    private java.util.List<MomentLike> likes;

    /**
     * 动态评论列表（不参与数据库映射）
     */
    @Transient
    private java.util.List<MomentComment> comments;
} 