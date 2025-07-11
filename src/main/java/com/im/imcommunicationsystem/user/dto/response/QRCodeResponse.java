package com.im.imcommunicationsystem.user.dto.response;

import java.time.LocalDateTime;

/**
 * 二维码响应DTO
 */
public class QRCodeResponse {

    private String qrCodeData;
    private String qrCodeImageUrl;
    private String qrCodeBase64;
    private Long userId;
    private String userIdString;
    private String userNickname;
    private String userAvatarUrl;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;

    // 构造函数
    public QRCodeResponse() {}

    public QRCodeResponse(String qrCodeData, String qrCodeImageUrl, String qrCodeBase64, 
                         Long userId, String userIdString, String userNickname, String userAvatarUrl, 
                         LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this.qrCodeData = qrCodeData;
        this.qrCodeImageUrl = qrCodeImageUrl;
        this.qrCodeBase64 = qrCodeBase64;
        this.userId = userId;
        this.userIdString = userIdString;
        this.userNickname = userNickname;
        this.userAvatarUrl = userAvatarUrl;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    // Getter和Setter方法
    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public String getQrCodeImageUrl() {
        return qrCodeImageUrl;
    }

    public void setQrCodeImageUrl(String qrCodeImageUrl) {
        this.qrCodeImageUrl = qrCodeImageUrl;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserIdString() {
        return userIdString;
    }

    public void setUserIdString(String userIdString) {
        this.userIdString = userIdString;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "QRCodeResponse{" +
                "qrCodeData='" + qrCodeData + '\'' +
                ", qrCodeImageUrl='" + qrCodeImageUrl + '\'' +
                ", qrCodeBase64='" + qrCodeBase64 + '\'' +
                ", userId=" + userId +
                ", userIdString='" + userIdString + '\'' +
                ", userNickname='" + userNickname + '\'' +
                ", userAvatarUrl='" + userAvatarUrl + '\'' +
                ", generatedAt=" + generatedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}