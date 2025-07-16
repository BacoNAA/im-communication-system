package com.im.imcommunicationsystem.message.dto.request;

import com.im.imcommunicationsystem.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求对象
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * 会话ID（可选，如果不提供则需要提供recipientId来自动创建会话）
     */
    private Long conversationId;
    
    /**
     * 接收者ID（用于自动创建私聊会话，当conversationId为空时使用）
     */
    private Long recipientId;

    /**
     * 消息类型
     */
    @NotNull(message = "消息类型不能为空")
    private MessageType messageType;

    /**
     * 消息内容
     */
    @Size(max = 5000, message = "消息内容不能超过5000个字符")
    private String content;

    /**
     * 媒体文件ID（当消息类型为媒体消息时，关联用户模块的FileUpload）
     */
    private Long mediaFileId;

    /**
     * 回复的消息ID（可选）
     */
    private Long replyToMessageId;

    /**
     * 转发的消息ID（可选）
     */
    private Long forwardFromMessageId;

    /**
     * 消息元数据（JSON格式，可选）
     */
    private String metadata;

    /**
     * 临时消息ID（客户端生成，用于消息去重和状态跟踪）
     */
    private String tempMessageId;

    /**
     * 验证请求参数
     * 
     * @return 验证结果
     */
    public boolean isValid() {
        // 必须提供会话ID或接收者ID
        if (conversationId == null && recipientId == null) {
            return false;
        }
        
        // 消息类型不能为空
        if (messageType == null) {
            return false;
        }
        
        // 文本消息必须有内容
        if (messageType == MessageType.TEXT && (content == null || content.trim().isEmpty())) {
            return false;
        }
        
        // 媒体消息必须有媒体文件ID
        if (messageType.isMediaMessage() && mediaFileId == null) {
            return false;
        }
        
        // 回复消息和转发消息不能同时存在
        if (replyToMessageId != null && forwardFromMessageId != null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取验证错误信息
     * 
     * @return 错误信息列表
     */
    public java.util.List<String> getValidationErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        if (conversationId == null && recipientId == null) {
            errors.add("必须提供会话ID或接收者ID");
        }
        
        if (messageType == null) {
            errors.add("消息类型不能为空");
        }
        
        if (messageType == MessageType.TEXT && (content == null || content.trim().isEmpty())) {
            errors.add("文本消息内容不能为空");
        }
        
        if (messageType != null && messageType.isMediaMessage() && mediaFileId == null) {
            errors.add("媒体消息必须提供媒体文件ID");
        }
        
        if (replyToMessageId != null && forwardFromMessageId != null) {
            errors.add("回复消息和转发消息不能同时存在");
        }
        
        return errors;
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
     * 检查是否为媒体消息
     * 
     * @return 是否为媒体消息
     */
    public boolean isMediaMessage() {
        return messageType != null && messageType.isMediaMessage();
    }

    /**
     * 获取有效内容
     * 
     * @return 有效内容
     */
    public String getEffectiveContent() {
        return content != null ? content.trim() : "";
    }
}