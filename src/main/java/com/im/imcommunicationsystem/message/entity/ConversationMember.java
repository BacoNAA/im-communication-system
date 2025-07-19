package com.im.imcommunicationsystem.message.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 会话成员实体类
 * 表示用户在会话中的成员信息
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "conversation_members", indexes = {
        @Index(name = "idx_conversation_members_user_id", columnList = "user_id"),
        @Index(name = "idx_conversation_members_is_pinned", columnList = "is_pinned")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ConversationMemberId.class)
public class ConversationMember {

    /**
     * 会话ID
     */
    @Id
    @Column(name = "conversation_id")
    private Long conversationId;

    /**
     * 用户ID
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * 是否置顶此会话
     */
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * 是否归档此会话
     */
    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    /**
     * 是否为此会话开启免打扰
     */
    @Column(name = "is_dnd", nullable = false)
    @Builder.Default
    private Boolean isDnd = false;

    /**
     * 在此会话中未发送的草稿
     */
    @Column(name = "draft", columnDefinition = "TEXT")
    private String draft;

    /**
     * 最后已读消息的ID
     */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    /**
     * 最后可接受消息的ID
     */
    @Column(name = "last_acceptable_message_id")
    private Long lastAcceptableMessageId;

    /**
     * 检查是否置顶此会话
     * 
     * @return 是否置顶
     */
    public boolean isPinned() {
        return Boolean.TRUE.equals(isPinned);
    }

    /**
     * 检查是否归档此会话
     * 
     * @return 是否归档
     */
    public boolean isArchived() {
        return Boolean.TRUE.equals(isArchived);
    }

    /**
     * 检查是否开启免打扰
     * 
     * @return 是否开启免打扰
     */
    public boolean isDndEnabled() {
        return Boolean.TRUE.equals(isDnd);
    }

    /**
     * 检查是否有草稿内容
     * 
     * @return 是否有草稿
     */
    public boolean hasDraft() {
        return draft != null && !draft.trim().isEmpty();
    }
}