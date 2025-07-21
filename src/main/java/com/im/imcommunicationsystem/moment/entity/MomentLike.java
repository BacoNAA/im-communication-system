package com.im.imcommunicationsystem.moment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 动态点赞实体类
 */
@Data
@Entity
@Table(name = "moment_likes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentLike {

    /**
     * 点赞唯一标识
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
     * 点赞用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 点赞时间
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
} 