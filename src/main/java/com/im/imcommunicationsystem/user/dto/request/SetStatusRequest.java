package com.im.imcommunicationsystem.user.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 设置用户状态请求DTO
 */
public class SetStatusRequest {

    @Size(max = 100, message = "状态内容长度不能超过100个字符")
    private String statusText;

    private String statusType; // 状态类型：online, busy, away, invisible

    private LocalDateTime expiresAt; // 状态过期时间

    private String emoji; // 状态表情

    // 构造函数
    public SetStatusRequest() {}

    public SetStatusRequest(String statusText, String statusType, LocalDateTime expiresAt, String emoji) {
        this.statusText = statusText;
        this.statusType = statusType;
        this.expiresAt = expiresAt;
        this.emoji = emoji;
    }

    // Getter和Setter方法
    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getStatusType() {
        return statusType;
    }

    public void setStatusType(String statusType) {
        this.statusType = statusType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Override
    public String toString() {
        return "SetStatusRequest{" +
                "statusText='" + statusText + '\'' +
                ", statusType='" + statusType + '\'' +
                ", expiresAt=" + expiresAt +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}