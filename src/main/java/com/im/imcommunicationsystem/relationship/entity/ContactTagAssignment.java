package com.im.imcommunicationsystem.relationship.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 联系人标签分配实体类
 * 对应数据库表：contact_tag_assignments
 * 管理联系人和标签的多对多关系
 */
@Entity
@Table(name = "contact_tag_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id", "tag_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactTagAssignment {

    /**
     * 分配记录ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 好友ID
     */
    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    /**
     * 标签ID
     */
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    /**
     * 分配时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 关联的标签实体
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private ContactTag contactTag;
}