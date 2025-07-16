package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存草稿请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDraftRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 草稿内容
     */
    @Size(max = 5000, message = "草稿内容不能超过5000个字符")
    private String content;

    /**
     * 草稿类型（text, rich_text等）
     */
    private String contentType;

    /**
     * 是否清空草稿
     */
    private Boolean clear;

    /**
     * 检查是否为清空草稿操作
     * 
     * @return 是否清空草稿
     */
    public boolean isClearDraft() {
        return clear != null && clear;
    }

    /**
     * 检查草稿内容是否为空
     * 
     * @return 草稿是否为空
     */
    public boolean isEmptyContent() {
        return content == null || content.trim().isEmpty();
    }
}