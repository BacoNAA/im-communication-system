package com.im.imcommunicationsystem.relationship.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 好友请求创建请求DTO
 */
@Data
public class ContactRequestCreateRequest {

    /**
     * 请求者ID
     */
    @NotNull(message = "请求者ID不能为空")
    private Long requesterId;

    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long recipientId;

    /**
     * 验证信息
     */
    @Size(max = 200, message = "验证信息不能超过200个字符")
    private String verificationMessage;

    /**
     * 来源（可选）
     */
    @Size(max = 50, message = "来源信息不能超过50个字符")
    private String source;
}