package com.im.imcommunicationsystem.message.dto.response;

import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话成员DTO
 * 用于传输会话成员信息
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDTO {

    /**
     * 备注名
     */
    private String alias;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户信息
     */
    private UserProfileResponse user;

    /**
     * 是否置顶此会话
     */
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * 是否归档此会话
     */
    @Builder.Default
    private Boolean isArchived = false;

    /**
     * 是否为此会话开启免打扰
     */
    @Builder.Default
    private Boolean isDnd = false;

    /**
     * 在此会话中未发送的草稿
     */
    private String draft;

    /**
     * 最后已读消息的ID
     */
    private Long lastReadMessageId;

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

    /**
     * 获取用户头像URL
     * 
     * @return 头像URL
     */
    public String getAvatarUrl() {
        return user != null ? user.getAvatarUrl() : null;
    }

    /**
     * 获取显示昵称
     * 使用用户昵称，如果没有则使用用户邮箱
     * 
     * @return 显示昵称
     */
    public String getDisplayNickname() {
        if (alias != null && !alias.trim().isEmpty()) {
            return alias;
        }
        if (user != null && user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        
        if (user != null && user.getEmail() != null) {
            return user.getEmail();
        }
        
        return "未知用户";
    }
}