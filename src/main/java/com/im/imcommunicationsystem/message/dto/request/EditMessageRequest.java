package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * 编辑消息请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditMessageRequest {

    /**
     * 消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 新的消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过5000个字符")
    private String content;

    /**
     * 消息类型（text, rich_text等）
     */
    private String contentType;

    /**
     * 编辑原因
     */
    private String editReason;

    /**
     * 消息元数据
     */
    private Map<String, Object> metadata;

    /**
     * 检查内容是否有实际变化
     * 
     * @param originalContent 原始内容
     * @return 是否有变化
     */
    public boolean hasContentChanged(String originalContent) {
        if (originalContent == null && content == null) {
            return false;
        }
        if (originalContent == null || content == null) {
            return true;
        }
        return !originalContent.equals(content.trim());
    }
}