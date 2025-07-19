package com.im.imcommunicationsystem.message.dto.response;

import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息数据传输对象
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
public class MessageDTO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 媒体文件ID
     */
    private Long mediaFileId;

    /**
     * 媒体文件信息（来自用户模块的FileUpload）
     */
    private com.im.imcommunicationsystem.user.entity.FileUpload mediaFile;

    /**
     * 回复的消息ID
     */
    private Long replyToMessageId;

    /**
     * 回复的消息信息
     */
    private MessageDTO replyToMessage;

    /**
     * 转发的消息ID
     */
    private Long forwardFromMessageId;

    /**
     * 转发的消息信息
     */
    private MessageDTO forwardFromMessage;

    /**
     * 消息状态
     */
    private MessageStatus status;

    /**
     * 是否已编辑
     */
    private Boolean edited;

    /**
     * 编辑时间
     */
    private LocalDateTime editedAt;

    /**
     * 消息元数据
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
     * 是否为当前用户发送的消息
     */
    private Boolean isSentByCurrentUser;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 获取消息是否已读
     * 
     * @return 是否已读
     */
    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }
    
    /**
     * 设置消息是否已读
     * 
     * @param isRead 是否已读
     */
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead != null ? isRead : false;
    }

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 检查是否为媒体消息
     * 
     * @return 是否为媒体消息
     */
    public boolean isMediaMessage() {
        return messageType != null && messageType.isMediaMessage();
    }

    /**
     * 检查是否为系统消息
     * 
     * @return 是否为系统消息
     */
    public boolean isSystemMessage() {
        return messageType != null && messageType.isSystemMessage();
    }

    /**
     * 检查是否为回复消息
     * 
     * @return 是否为回复消息
     */
    public boolean isReplyMessage() {
        return replyToMessageId != null;
    }

    /**
     * 检查是否为转发消息
     * 
     * @return 是否为转发消息
     */
    public boolean isForwardMessage() {
        return forwardFromMessageId != null;
    }

    /**
     * 检查是否已编辑
     * 
     * @return 是否已编辑
     */
    public boolean isEdited() {
        return edited != null && edited;
    }

    /**
     * 检查是否可以撤回
     * 
     * @return 是否可以撤回
     */
    public boolean canRecall() {
        return status != null && status.canRecall();
    }

    /**
     * 检查是否可以编辑
     * 
     * @return 是否可以编辑
     */
    public boolean canEdit() {
        return status != null && status.canEdit() && messageType == MessageType.TEXT;
    }

    /**
     * 检查是否可以删除
     * 
     * @return 是否可以删除
     */
    public boolean canDelete() {
        return status != null && status.canDelete();
    }

    /**
     * 获取显示内容
     * 
     * @return 显示内容
     */
    public String getDisplayContent() {
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        
        if (messageType != null) {
            switch (messageType) {
                case IMAGE:
                    return "[图片]";
                case VIDEO:
                    return "[视频]";
                case AUDIO:
                    return "[语音]";
                case FILE:
                    return "[文件]";
                case LOCATION:
                    return "[位置]";
                case EMOJI:
                    return "[表情]";
                case SYSTEM:
                    return "[系统消息]";
                case NOTIFICATION:
                    return "[通知]";
                case RECALL:
                    return "[消息已撤回]";
                default:
                    return "[未知消息类型]";
            }
        }
        
        return "";
    }
}