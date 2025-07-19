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
     * 要转发的消息ID列表
     */
    @NotEmpty(message = "消息ID列表不能为空")
    @Size(max = 50, message = "一次最多转发50条消息")
    private List<Long> messageIds;

    /**
     * 目标会话ID列表
     */
    @NotEmpty(message = "目标会话不能为空")
    @Size(max = 10, message = "一次最多转发到10个会话")
    private List<Long> targetConversationIds;

    /**
     * 转发时的附加评论
     */
    @Size(max = 500, message = "附加评论不能超过500个字符")
    private String comment;

    /**
     * 转发类型（MERGE: 合并转发, SEPARATE: 逐条转发）
     */
    @NotNull(message = "转发类型不能为空")
    private ForwardType forwardType;

    /**
     * 转发类型枚举
     */
    public enum ForwardType {
        /**
         * 合并转发 - 将多条消息合并为一条转发
         */
        MERGE,
        
        /**
         * 逐条转发 - 每条消息单独转发
         */
        SEPARATE
    }

    /**
     * 检查是否为合并转发
     * 
     * @return 是否为合并转发
     */
    public boolean isMergeForward() {
        return ForwardType.MERGE.equals(forwardType);
    }

    /**
     * 检查是否为逐条转发
     * 
     * @return 是否为逐条转发
     */
    public boolean isSeparateForward() {
        return ForwardType.SEPARATE.equals(forwardType);
    }

    /**
     * 检查是否有附加评论
     * 
     * @return 是否有附加评论
     */
    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }
}