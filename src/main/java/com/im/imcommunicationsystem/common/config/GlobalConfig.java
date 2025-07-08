package com.im.imcommunicationsystem.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 全局配置类
 * 统一管理应用程序的全局配置参数
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class GlobalConfig {

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    private String name = "IM Communication System";

    /**
     * 应用版本
     */
    @NotBlank(message = "应用版本不能为空")
    private String version = "1.0.0";

    /**
     * 应用描述
     */
    private String description = "即时通讯系统";

    /**
     * 文件上传配置
     */
    @NotNull(message = "文件上传配置不能为空")
    private FileUpload fileUpload = new FileUpload();



    /**
     * 验证码配置
     */
    @NotNull(message = "验证码配置不能为空")
    private VerificationCode verificationCode = new VerificationCode();

    /**
     * 消息配置
     */
    @NotNull(message = "消息配置不能为空")
    private Message message = new Message();

    /**
     * 文件上传配置类
     */
    public static class FileUpload {
        /**
         * 最大文件大小（MB）
         */
        @Positive(message = "最大文件大小必须为正数")
        private int maxFileSize = 100;

        /**
         * 最大图片大小（MB）
         */
        @Positive(message = "最大图片大小必须为正数")
        private int maxImageSize = 10;

        /**
         * 最大视频大小（MB）
         */
        @Positive(message = "最大视频大小必须为正数")
        private int maxVideoSize = 500;

        /**
         * 允许的图片格式
         */
        private String[] allowedImageTypes = {"jpg", "jpeg", "png", "gif", "webp"};

        /**
         * 允许的视频格式
         */
        private String[] allowedVideoTypes = {"mp4", "avi", "mov", "wmv", "flv"};

        /**
         * 允许的文档格式
         */
        private String[] allowedDocumentTypes = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"};

        /**
         * 文件存储路径
         */
        @NotBlank(message = "文件存储路径不能为空")
        private String uploadPath = "/uploads";

        // Getters and Setters
        public int getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(int maxFileSize) { this.maxFileSize = maxFileSize; }
        public int getMaxImageSize() { return maxImageSize; }
        public void setMaxImageSize(int maxImageSize) { this.maxImageSize = maxImageSize; }
        public int getMaxVideoSize() { return maxVideoSize; }
        public void setMaxVideoSize(int maxVideoSize) { this.maxVideoSize = maxVideoSize; }
        public String[] getAllowedImageTypes() { return allowedImageTypes; }
        public void setAllowedImageTypes(String[] allowedImageTypes) { this.allowedImageTypes = allowedImageTypes; }
        public String[] getAllowedVideoTypes() { return allowedVideoTypes; }
        public void setAllowedVideoTypes(String[] allowedVideoTypes) { this.allowedVideoTypes = allowedVideoTypes; }
        public String[] getAllowedDocumentTypes() { return allowedDocumentTypes; }
        public void setAllowedDocumentTypes(String[] allowedDocumentTypes) { this.allowedDocumentTypes = allowedDocumentTypes; }
        public String getUploadPath() { return uploadPath; }
        public void setUploadPath(String uploadPath) { this.uploadPath = uploadPath; }
    }



    /**
     * 验证码配置类
     */
    public static class VerificationCode {
        /**
         * 验证码长度
         */
        @Positive(message = "验证码长度必须为正数")
        private int length = 6;

        /**
         * 验证码过期时间（分钟）
         */
        @Positive(message = "验证码过期时间必须为正数")
        private int expiration = 5;

        /**
         * 同一邮箱发送间隔（秒）
         */
        @Positive(message = "发送间隔必须为正数")
        private int sendInterval = 60;

        /**
         * 每日最大发送次数
         */
        @Positive(message = "每日最大发送次数必须为正数")
        private int maxDailySend = 10;

        // Getters and Setters
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public int getExpiration() { return expiration; }
        public void setExpiration(int expiration) { this.expiration = expiration; }
        public int getSendInterval() { return sendInterval; }
        public void setSendInterval(int sendInterval) { this.sendInterval = sendInterval; }
        public int getMaxDailySend() { return maxDailySend; }
        public void setMaxDailySend(int maxDailySend) { this.maxDailySend = maxDailySend; }
    }

    /**
     * 消息配置类
     */
    public static class Message {
        /**
         * 消息撤回时间限制（分钟）
         */
        @Positive(message = "消息撤回时间限制必须为正数")
        private int recallTimeLimit = 2;

        /**
         * 消息最大长度
         */
        @Positive(message = "消息最大长度必须为正数")
        private int maxLength = 5000;

        /**
         * 群聊最大成员数
         */
        @Positive(message = "群聊最大成员数必须为正数")
        private int maxGroupMembers = 500;

        /**
         * 消息历史保存天数
         */
        @Positive(message = "消息历史保存天数必须为正数")
        private int historyRetentionDays = 365;

        // Getters and Setters
        public int getRecallTimeLimit() { return recallTimeLimit; }
        public void setRecallTimeLimit(int recallTimeLimit) { this.recallTimeLimit = recallTimeLimit; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        public int getMaxGroupMembers() { return maxGroupMembers; }
        public void setMaxGroupMembers(int maxGroupMembers) { this.maxGroupMembers = maxGroupMembers; }
        public int getHistoryRetentionDays() { return historyRetentionDays; }
        public void setHistoryRetentionDays(int historyRetentionDays) { this.historyRetentionDays = historyRetentionDays; }
    }

    // Main class Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FileUpload getFileUpload() { return fileUpload; }
    public void setFileUpload(FileUpload fileUpload) { this.fileUpload = fileUpload; }

    public VerificationCode getVerificationCode() { return verificationCode; }
    public void setVerificationCode(VerificationCode verificationCode) { this.verificationCode = verificationCode; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
}