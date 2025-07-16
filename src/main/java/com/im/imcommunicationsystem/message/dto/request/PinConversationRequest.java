package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 置顶会话请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinConversationRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 是否置顶
     */
    @NotNull(message = "置顶状态不能为空")
    private Boolean pinned;

    /**
     * 置顶排序权重（数值越大越靠前）
     */
    private Integer pinOrder;
}