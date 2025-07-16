package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 撤回消息请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallMessageRequest {

    /**
     * 消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 撤回原因
     */
    private String reason;

    /**
     * 是否对所有人撤回（管理员权限）
     */
    private Boolean recallForAll;

    /**
     * 检查是否为管理员撤回
     * 
     * @return 是否为管理员撤回
     */
    public boolean isAdminRecall() {
        return recallForAll != null && recallForAll;
    }
}