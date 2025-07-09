package com.im.imcommunicationsystem.user.dto.response;

import java.util.Map;

/**
 * 用户设置响应DTO
 */
public class UserSettingsResponse {

    private Long userId;
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
    private String theme;
    private String language;
    private Integer fontSize;
    
    // 安全设置
    private Boolean twoFactorEnabled;
    private Boolean loginNotifications;

    // 构造函数
    public UserSettingsResponse() {}

    public UserSettingsResponse(Long userId, Map<String, Object> settings) {
        this.userId = userId;
        this.settings = settings;
    }

    // Getter和Setter方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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
        return "UserSettingsResponse{" +
                "userId=" + userId +
                ", settings=" + settings +
                ", showOnlineStatus=" + showOnlineStatus +
                ", allowFriendRequests=" + allowFriendRequests +
                ", showLastSeen=" + showLastSeen +
                ", enableNotifications=" + enableNotifications +
                ", enableSoundNotifications=" + enableSoundNotifications +
                ", enableVibration=" + enableVibration +
                ", theme='" + theme + '\'' +
                ", language='" + language + '\'' +
                ", fontSize=" + fontSize +
                ", twoFactorEnabled=" + twoFactorEnabled +
                ", loginNotifications=" + loginNotifications +
                '}';
    }
}