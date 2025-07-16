package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 转发消息请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardMessageRequest {

    /**
     * 要转发的消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 目标会话ID列表
     */
    @NotEmpty(message = "目标会话不能为空")
    @Size(max = 10, message = "一次最多转发到10个会话")
    private List<Long> targetConversationIds;

    /**
     * 转发时的附加消息
     */
    @Size(max = 500, message = "附加消息不能超过500个字符")
    private String additionalMessage;

    /**
     * 是否保留原始发送者信息
     */
    private Boolean keepOriginalSender;

    /**
     * 转发类型（quote: 引用转发, copy: 复制转发）
     */
    private String forwardType;

    /**
     * 检查是否为引用转发
     * 
     * @return 是否为引用转发
     */
    public boolean isQuoteForward() {
        return "quote".equals(forwardType);
    }

    /**
     * 检查是否为复制转发
     * 
     * @return 是否为复制转发
     */
    public boolean isCopyForward() {
        return "copy".equals(forwardType);
    }

    /**
     * 检查是否有附加消息
     * 
     * @return 是否有附加消息
     */
    public boolean hasAdditionalMessage() {
        return additionalMessage != null && !additionalMessage.trim().isEmpty();
    }
}