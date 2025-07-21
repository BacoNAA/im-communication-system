package com.im.imcommunicationsystem.moment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态评论实体类
 */
@Data
@Entity
@Table(name = "moment_comments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentComment {

    /**
     * 评论唯一标识
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联动态ID
     */
    @Column(name = "moment_id", nullable = false)
    private Long momentId;

    /**
     * 评论用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 评论内容
     */
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /**
     * 父评论ID（回复功能）
     */
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    /**
     * 回复目标用户ID
     */
    @Column(name = "reply_to_user_id")
    private Long replyToUserId;

    /**
     * 是否私密评论
     */
    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;

    /**
     * 评论时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 关联动态（不参与数据库映射）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", insertable = false, updatable = false)
    private Moment moment;

    /**
     * 父评论（不参与数据库映射）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private MomentComment parentComment;

    /**
     * 回复评论列表（不参与数据库映射）
     */
    @OneToMany(mappedBy = "parentComment")
    private List<MomentComment> replies;
} 