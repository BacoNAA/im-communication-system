package com.im.imcommunicationsystem.relationship.dto.response;

import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求响应DTO
 */
@Data
public class ContactRequestResponse {

    /**
     * 请求ID
     */
    private Long requestId;

    /**
     * 请求者ID
     */
    private Long requesterId;

    /**
     * 请求者用户名
     */
    private String requesterUsername;

    /**
     * 请求者昵称
     */
    private String requesterNickname;

    /**
     * 请求者头像URL
     */
    private String requesterAvatarUrl;

    /**
     * 接收者ID
     */
    private Long recipientId;

    /**
     * 接收者用户名
     */
    private String recipientUsername;

    /**
     * 接收者昵称
     */
    private String recipientNickname;

    /**
     * 接收者头像URL
     */
    private String recipientAvatarUrl;

    /**
     * 验证信息
     */
    private String verificationMessage;

    /**
     * 请求状态
     */
    private ContactRequestStatus status;

    /**
     * 请求状态描述
     */
    private String statusDescription;

    /**
     * 来源
     */
    private String source;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 是否可以操作（接受/拒绝）
     */
    private Boolean canProcess;

    /**
     * 是否可以撤回
     */
    private Boolean canWithdraw;
}