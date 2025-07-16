package com.im.imcommunicationsystem.message.dto.response;

import com.im.imcommunicationsystem.auth.dto.response.UserInfoResponse;
import com.im.imcommunicationsystem.message.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话数据传输对象
 * 用于前后端数据传输
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 会话类型
     */
    private ConversationType type;

    /**
     * 会话名称
     */
    private String name;

    /**
     * 会话描述
     */
    private String description;

    /**
     * 会话头像URL
     */
    private String avatarUrl;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者信息
     */
    private UserInfoResponse creator;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 最后一条消息ID
     */
    private Long lastMessageId;

    /**
     * 最后一条消息
     */
    private MessageDTO lastMessage;

    /**
     * 会话设置
     */
    private String settings;

    /**
     * 会话元数据
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 参与者列表
     */
    private List<ConversationMemberDTO> participants;

    /**
     * 参与者数量
     */
    private Integer participantCount;

    /**
     * 当前用户在此会话中的信息
     */
    private ConversationMemberDTO currentUserParticipant;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 是否置顶
     */
    private Boolean isPinned;

    /**
     * 是否归档
     */
    private Boolean isArchived;

    /**
     * 是否开启勿扰模式
     */
    private Boolean isDnd;

    /**
     * 草稿内容
     */
    private String draft;

    /**
     * 检查是否为私聊
     * 
     * @return 是否为私聊
     */
    public boolean isPrivate() {
        return type != null && type.isPrivate();
    }

    /**
     * 检查是否为群聊
     * 
     * @return 是否为群聊
     */
    public boolean isGroup() {
        return type != null && type.isGroup();
    }

    /**
     * 检查是否为系统会话
     * 
     * @return 是否为系统会话
     */
    public boolean isSystem() {
        return type != null && type.isSystem();
    }

    /**
     * 检查是否有未读消息
     * 
     * @return 是否有未读消息
     */
    public boolean hasUnreadMessages() {
        return unreadCount != null && unreadCount > 0;
    }

    /**
     * 检查是否置顶
     * 
     * @return 是否置顶
     */
    public boolean isPinned() {
        return isPinned != null && isPinned;
    }

    /**
     * 检查是否归档
     * 
     * @return 是否归档
     */
    public boolean isArchived() {
        return isArchived != null && isArchived;
    }

    /**
     * 检查是否开启勿扰模式
     * 
     * @return 是否开启勿扰模式
     */
    public boolean isDndEnabled() {
        return isDnd != null && isDnd;
    }

    /**
     * 检查是否有草稿
     * 
     * @return 是否有草稿
     */
    public boolean hasDraft() {
        return draft != null && !draft.trim().isEmpty();
    }

    /**
     * 获取显示名称
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        
        if (isPrivate() && participants != null && participants.size() == 2) {
            // 私聊显示对方的昵称
            for (ConversationMemberDTO participant : participants) {
                if (currentUserParticipant != null && 
                    !participant.getUserId().equals(currentUserParticipant.getUserId())) {
                    return participant.getDisplayNickname();
                }
            }
        }
        
        return "未命名会话";
    }

    /**
     * 获取显示头像
     * 
     * @return 显示头像URL
     */
    public String getDisplayAvatar() {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            return avatarUrl;
        }
        
        if (isPrivate() && participants != null && participants.size() == 2) {
            // 私聊显示对方的头像
            for (ConversationMemberDTO participant : participants) {
                if (currentUserParticipant != null && 
                    !participant.getUserId().equals(currentUserParticipant.getUserId())) {
                    return participant.getUser().getAvatarUrl();
                }
            }
        }
        
        return null;
    }

    /**
     * 获取最后消息的显示内容
     * 
     * @return 最后消息的显示内容
     */
    public String getLastMessageDisplay() {
        if (lastMessage == null) {
            return "";
        }
        
        String senderName = "";
        if (!isPrivate() && lastMessage.getSenderNickname() != null) {
            senderName = lastMessage.getSenderNickname() + ": ";
        }
        
        return senderName + lastMessage.getDisplayContent();
    }
}