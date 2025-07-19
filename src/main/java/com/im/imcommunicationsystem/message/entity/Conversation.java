package com.im.imcommunicationsystem.message.entity;

import com.im.imcommunicationsystem.message.enums.ConversationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话实体类
 * 表示系统中的会话信息
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversation_type", columnList = "conversation_type"),
        @Index(name = "idx_created_by", columnList = "created_by"),
        @Index(name = "idx_last_active", columnList = "last_active_at"),
        @Index(name = "idx_deleted", columnList = "deleted")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    /**
     * 会话ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    private ConversationType conversationType;

    /**
     * 会话名称
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 会话描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 会话头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 关联的群组ID（如果是群聊）
     */
    @Column(name = "related_group_id")
    private Long relatedGroupId;

    /**
     * 创建者ID
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * 最后活跃时间
     */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    /**
     * 最后一条消息ID
     */
    @Column(name = "last_message_id")
    private Long lastMessageId;

    /**
     * 是否已删除
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * 会话设置（JSON格式）
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /**
     * 会话元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

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
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 实体创建前的处理
     */
    @PrePersist
    protected void onCreate() {
        // 初始化最后活跃时间为当前时间
        if (lastActiveAt == null) {
            lastActiveAt = LocalDateTime.now();
        }
        
        // 初始化其他默认值
        if (deleted == null) {
            deleted = false;
        }
    }

    /**
     * 会话参与者列表
     */
    @OneToMany(mappedBy = "conversationId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationMember> participants;

    /**
     * 检查是否为私聊会话
     * 
     * @return 是否为私聊
     */
    public boolean isPrivateConversation() {
        return conversationType == ConversationType.PRIVATE;
    }

    /**
     * 检查是否为群聊会话
     * 
     * @return 是否为群聊
     */
    public boolean isGroupConversation() {
        return conversationType == ConversationType.GROUP;
    }

    /**
     * 检查是否为系统会话
     * 
     * @return 是否为系统会话
     */
    public boolean isSystemConversation() {
        return conversationType == ConversationType.SYSTEM;
    }

    /**
     * 检查会话是否已删除
     * 
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return deleted || deletedAt != null;
    }

    /**
     * 获取会话显示名称
     * 如果没有设置名称，则根据会话类型返回默认名称
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        
        switch (conversationType) {
            case PRIVATE:
                return "私聊";
            case GROUP:
                return "群聊";
            case SYSTEM:
                return "系统消息";
            default:
                return "未知会话";
        }
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 软删除会话
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}