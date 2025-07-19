package com.im.imcommunicationsystem.user.dto.request;

import java.util.Map;

/**
 * 更新用户设置请求DTO
 */
public class UpdateSettingsRequest {

    private Map<String, Object> settings;

    // 隐私设置
    private Boolean showOnlineStatus;
    private Boolean allowFriendRequests;
    private Boolean showLastSeen;
    
    // 通知设置
    private Boolean enableNotifications;
    private Boolean enableSoundNotifications;
    private Boolean enableVibration;
    
    // 聊天设置
    private String theme; // light, dark, auto
    private String themeColor; // 主题颜色，如 "#1890ff"
    private String chatBackground; // 聊天背景，可以是颜色值或图片URL
    private String language; // zh-CN, en-US, etc.
    private Integer fontSize;
    
    // 安全设置
    private Boolean twoFactorEnabled;
    private Boolean loginNotifications;

    // 构造函数
    public UpdateSettingsRequest() {}

    public UpdateSettingsRequest(Map<String, Object> settings) {
        this.settings = settings;
    }

    // Getter和Setter方法
    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public Boolean getShowOnlineStatus() {
        return showOnlineStatus;
    }

    public void setShowOnlineStatus(Boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }

    public Boolean getAllowFriendRequests() {
        return allowFriendRequests;
    }

    public void setAllowFriendRequests(Boolean allowFriendRequests) {
        this.allowFriendRequests = allowFriendRequests;
    }

    public Boolean getShowLastSeen() {
        return showLastSeen;
    }

    public void setShowLastSeen(Boolean showLastSeen) {
        this.showLastSeen = showLastSeen;
    }

    public Boolean getEnableNotifications() {
        return enableNotifications;
    }

    public void setEnableNotifications(Boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    public Boolean getEnableSoundNotifications() {
        return enableSoundNotifications;
    }

    public void setEnableSoundNotifications(Boolean enableSoundNotifications) {
        this.enableSoundNotifications = enableSoundNotifications;
    }

    public Boolean getEnableVibration() {
        return enableVibration;
    }

    public void setEnableVibration(Boolean enableVibration) {
        this.enableVibration = enableVibration;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getChatBackground() {
        return chatBackground;
    }

    public void setChatBackground(String chatBackground) {
        this.chatBackground = chatBackground;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public Boolean getLoginNotifications() {
        return loginNotifications;
    }

    public void setLoginNotifications(Boolean loginNotifications) {
        this.loginNotifications = loginNotifications;
    }

    @Override
    public String toString() {
        return "UpdateSettingsRequest{" +
                "settings=" + settings +
                ", showOnlineStatus=" + showOnlineStatus +
                ", allowFriendRequests=" + allowFriendRequests +
                ", showLastSeen=" + showLastSeen +
                ", enableNotifications=" + enableNotifications +
                ", enableSoundNotifications=" + enableSoundNotifications +
                ", enableVibration=" + enableVibration +
                ", theme='" + theme + '\'' +
                ", themeColor='" + themeColor + '\'' +
                ", chatBackground='" + chatBackground + '\'' +
                ", language='" + language + '\'' +
                ", fontSize=" + fontSize +
                ", twoFactorEnabled=" + twoFactorEnabled +
                ", loginNotifications=" + loginNotifications +
                '}';
    }
}