package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 标记消息已读请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 要标记为已读的消息ID列表（可选，如果为空则标记会话中所有未读消息）
     */
    private List<Long> messageIds;

    /**
     * 最后已读消息ID（用于批量标记到某条消息为止的所有消息为已读）
     */
    private Long lastReadMessageId;

    /**
     * 是否标记会话中所有消息为已读
     */
    private Boolean markAllAsRead;

    /**
     * 检查是否为标记所有消息已读
     * 
     * @return 是否标记所有消息已读
     */
    public boolean isMarkAllAsRead() {
        return markAllAsRead != null && markAllAsRead;
    }

    /**
     * 检查是否为批量标记到指定消息
     * 
     * @return 是否批量标记
     */
    public boolean isBatchMarkToMessage() {
        return lastReadMessageId != null;
    }

    /**
     * 检查是否为指定消息列表标记
     * 
     * @return 是否指定消息列表
     */
    public boolean isSpecificMessages() {
        return messageIds != null && !messageIds.isEmpty();
    }
}