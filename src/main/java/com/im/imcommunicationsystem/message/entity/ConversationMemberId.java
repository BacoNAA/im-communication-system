package com.im.imcommunicationsystem.message.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 会话成员复合主键类
 * 用于ConversationMember实体的复合主键
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMemberId that = (ConversationMemberId) o;
        return Objects.equals(conversationId, that.conversationId) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }

    @Override
    public String toString() {
        return "ConversationMemberId{" +
                "conversationId=" + conversationId +
                ", userId=" + userId +
                '}';
    }
}