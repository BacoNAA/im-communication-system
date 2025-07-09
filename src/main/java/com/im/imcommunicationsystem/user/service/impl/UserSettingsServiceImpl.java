package com.im.imcommunicationsystem.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.user.dto.request.UpdateSettingsRequest;
import com.im.imcommunicationsystem.user.dto.response.UserSettingsResponse;
import com.im.imcommunicationsystem.user.entity.UserSettings;
import com.im.imcommunicationsystem.user.repository.UserSettingsRepository;
import com.im.imcommunicationsystem.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户设置服务实现类
 * 实现用户个性化设置的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public UserSettingsResponse getUserSettings(Long userId) {
        log.info("获取用户设置，用户ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        Optional<UserSettings> userSettingsOpt = userSettingsRepository.findByUserId(userId);
        
        if (userSettingsOpt.isEmpty()) {
            log.info("用户设置不存在，返回默认设置，用户ID: {}", userId);
            return createDefaultSettingsResponse(userId);
        }
        
        UserSettings userSettings = userSettingsOpt.get();
        return convertToResponse(userSettings);
    }

    @Override
    @Transactional
    public void updateUserSettings(Long userId, UpdateSettingsRequest request) {
        log.info("更新用户设置，用户ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (request == null) {
            throw new IllegalArgumentException("更新请求不能为空");
        }
        
        if (!validateSettings(request)) {
            throw new IllegalArgumentException("设置参数无效");
        }
        
        UserSettings userSettings = userSettingsRepository.findByUserId(userId)
                .orElse(new UserSettings());
        
        if (userSettings.getUserId() == null) {
            userSettings.setUserId(userId);
        }
        
        updateSettingsFromRequest(userSettings, request);
        userSettingsRepository.save(userSettings);
        
        log.info("用户设置更新成功，用户ID: {}", userId);
    }

    @Override
    @Transactional
    public void mergeSettings(Long userId, UpdateSettingsRequest request) {
        log.info("合并用户设置，用户ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (request == null) {
            throw new IllegalArgumentException("更新请求不能为空");
        }
        
        UserSettings userSettings = userSettingsRepository.findByUserId(userId)
                .orElse(new UserSettings());
        
        if (userSettings.getUserId() == null) {
            userSettings.setUserId(userId);
        }
        
        mergeSettingsFromRequest(userSettings, request);
        userSettingsRepository.save(userSettings);
        
        log.info("用户设置合并成功，用户ID: {}", userId);
    }

    @Override
    @Transactional
    public void resetToDefaultSettings(Long userId) {
        log.info("重置用户设置为默认值，用户ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setLanguage("zh-CN");
        userSettings.setPrivacySettings(createDefaultPrivacySettings());
        userSettings.setNotificationSettings(createDefaultNotificationSettings());
        userSettings.setThemeSettings(createDefaultThemeSettings());
        
        userSettingsRepository.save(userSettings);
        
        log.info("用户设置重置成功，用户ID: {}", userId);
    }

    @Override
    public boolean validateSettings(UpdateSettingsRequest request) {
        if (request == null) {
            return false;
        }
        
        // 验证主题设置
        if (request.getTheme() != null) {
            String theme = request.getTheme().toLowerCase();
            if (!"light".equals(theme) && !"dark".equals(theme) && !"auto".equals(theme)) {
                log.warn("无效的主题设置: {}", request.getTheme());
                return false;
            }
        }
        
        // 验证语言设置
        if (request.getLanguage() != null) {
            String language = request.getLanguage();
            if (!"zh-CN".equals(language) && !"en-US".equals(language)) {
                log.warn("无效的语言设置: {}", request.getLanguage());
                return false;
            }
        }
        
        // 验证字体大小
        if (request.getFontSize() != null) {
            Integer fontSize = request.getFontSize();
            if (fontSize < 12 || fontSize > 24) {
                log.warn("无效的字体大小: {}", request.getFontSize());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 创建默认设置响应
     */
    private UserSettingsResponse createDefaultSettingsResponse(Long userId) {
        UserSettingsResponse response = new UserSettingsResponse();
        response.setUserId(userId);
        response.setSettings(new HashMap<>());
        
        // 默认隐私设置
        response.setShowOnlineStatus(true);
        response.setAllowFriendRequests(true);
        response.setShowLastSeen(true);
        
        // 默认通知设置
        response.setEnableNotifications(true);
        response.setEnableSoundNotifications(true);
        response.setEnableVibration(true);
        
        // 默认聊天设置
        response.setTheme("light");
        response.setLanguage("zh-CN");
        response.setFontSize(14);
        
        // 默认安全设置
        response.setTwoFactorEnabled(false);
        response.setLoginNotifications(true);
        
        return response;
    }
    
    /**
     * 将实体转换为响应DTO
     */
    private UserSettingsResponse convertToResponse(UserSettings userSettings) {
        UserSettingsResponse response = new UserSettingsResponse();
        response.setUserId(userSettings.getUserId());
        response.setLanguage(userSettings.getLanguage());
        
        // 解析隐私设置
        Map<String, Object> privacySettings = parseJsonSettings(userSettings.getPrivacySettings());
        response.setShowOnlineStatus(getBooleanValue(privacySettings, "showOnlineStatus", true));
        response.setAllowFriendRequests(getBooleanValue(privacySettings, "allowFriendRequests", true));
        response.setShowLastSeen(getBooleanValue(privacySettings, "showLastSeen", true));
        
        // 解析通知设置
        Map<String, Object> notificationSettings = parseJsonSettings(userSettings.getNotificationSettings());
        response.setEnableNotifications(getBooleanValue(notificationSettings, "enableNotifications", true));
        response.setEnableSoundNotifications(getBooleanValue(notificationSettings, "enableSoundNotifications", true));
        response.setEnableVibration(getBooleanValue(notificationSettings, "enableVibration", true));
        response.setLoginNotifications(getBooleanValue(notificationSettings, "loginNotifications", true));
        
        // 解析主题设置
        Map<String, Object> themeSettings = parseJsonSettings(userSettings.getThemeSettings());
        response.setTheme(getStringValue(themeSettings, "theme", "light"));
        response.setFontSize(getIntegerValue(themeSettings, "fontSize", 14));
        response.setTwoFactorEnabled(getBooleanValue(themeSettings, "twoFactorEnabled", false));
        
        // 合并所有设置
        Map<String, Object> allSettings = new HashMap<>();
        allSettings.putAll(privacySettings);
        allSettings.putAll(notificationSettings);
        allSettings.putAll(themeSettings);
        response.setSettings(allSettings);
        
        return response;
    }
    
    /**
     * 从请求更新设置
     */
    private void updateSettingsFromRequest(UserSettings userSettings, UpdateSettingsRequest request) {
        // 更新语言设置
        if (request.getLanguage() != null) {
            userSettings.setLanguage(request.getLanguage());
        }
        
        // 更新隐私设置
        Map<String, Object> privacySettings = parseJsonSettings(userSettings.getPrivacySettings());
        updatePrivacySettings(privacySettings, request);
        userSettings.setPrivacySettings(toJsonString(privacySettings));
        
        // 更新通知设置
        Map<String, Object> notificationSettings = parseJsonSettings(userSettings.getNotificationSettings());
        updateNotificationSettings(notificationSettings, request);
        userSettings.setNotificationSettings(toJsonString(notificationSettings));
        
        // 更新主题设置
        Map<String, Object> themeSettings = parseJsonSettings(userSettings.getThemeSettings());
        updateThemeSettings(themeSettings, request);
        userSettings.setThemeSettings(toJsonString(themeSettings));
    }
    
    /**
     * 合并设置（只更新非空值）
     */
    private void mergeSettingsFromRequest(UserSettings userSettings, UpdateSettingsRequest request) {
        // 合并语言设置
        if (request.getLanguage() != null) {
            userSettings.setLanguage(request.getLanguage());
        }
        
        // 合并隐私设置
        Map<String, Object> privacySettings = parseJsonSettings(userSettings.getPrivacySettings());
        mergePrivacySettings(privacySettings, request);
        userSettings.setPrivacySettings(toJsonString(privacySettings));
        
        // 合并通知设置
        Map<String, Object> notificationSettings = parseJsonSettings(userSettings.getNotificationSettings());
        mergeNotificationSettings(notificationSettings, request);
        userSettings.setNotificationSettings(toJsonString(notificationSettings));
        
        // 合并主题设置
        Map<String, Object> themeSettings = parseJsonSettings(userSettings.getThemeSettings());
        mergeThemeSettings(themeSettings, request);
        userSettings.setThemeSettings(toJsonString(themeSettings));
    }
    
    /**
     * 更新隐私设置
     */
    private void updatePrivacySettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        settings.put("showOnlineStatus", request.getShowOnlineStatus() != null ? request.getShowOnlineStatus() : true);
        settings.put("allowFriendRequests", request.getAllowFriendRequests() != null ? request.getAllowFriendRequests() : true);
        settings.put("showLastSeen", request.getShowLastSeen() != null ? request.getShowLastSeen() : true);
    }
    
    /**
     * 合并隐私设置
     */
    private void mergePrivacySettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        if (request.getShowOnlineStatus() != null) {
            settings.put("showOnlineStatus", request.getShowOnlineStatus());
        }
        if (request.getAllowFriendRequests() != null) {
            settings.put("allowFriendRequests", request.getAllowFriendRequests());
        }
        if (request.getShowLastSeen() != null) {
            settings.put("showLastSeen", request.getShowLastSeen());
        }
    }
    
    /**
     * 更新通知设置
     */
    private void updateNotificationSettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        settings.put("enableNotifications", request.getEnableNotifications() != null ? request.getEnableNotifications() : true);
        settings.put("enableSoundNotifications", request.getEnableSoundNotifications() != null ? request.getEnableSoundNotifications() : true);
        settings.put("enableVibration", request.getEnableVibration() != null ? request.getEnableVibration() : true);
        settings.put("loginNotifications", request.getLoginNotifications() != null ? request.getLoginNotifications() : true);
    }
    
    /**
     * 合并通知设置
     */
    private void mergeNotificationSettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        if (request.getEnableNotifications() != null) {
            settings.put("enableNotifications", request.getEnableNotifications());
        }
        if (request.getEnableSoundNotifications() != null) {
            settings.put("enableSoundNotifications", request.getEnableSoundNotifications());
        }
        if (request.getEnableVibration() != null) {
            settings.put("enableVibration", request.getEnableVibration());
        }
        if (request.getLoginNotifications() != null) {
            settings.put("loginNotifications", request.getLoginNotifications());
        }
    }
    
    /**
     * 更新主题设置
     */
    private void updateThemeSettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        settings.put("theme", request.getTheme() != null ? request.getTheme() : "light");
        settings.put("fontSize", request.getFontSize() != null ? request.getFontSize() : 14);
        settings.put("twoFactorEnabled", request.getTwoFactorEnabled() != null ? request.getTwoFactorEnabled() : false);
    }
    
    /**
     * 合并主题设置
     */
    private void mergeThemeSettings(Map<String, Object> settings, UpdateSettingsRequest request) {
        if (request.getTheme() != null) {
            settings.put("theme", request.getTheme());
        }
        if (request.getFontSize() != null) {
            settings.put("fontSize", request.getFontSize());
        }
        if (request.getTwoFactorEnabled() != null) {
            settings.put("twoFactorEnabled", request.getTwoFactorEnabled());
        }
    }
    
    /**
     * 创建默认隐私设置
     */
    private String createDefaultPrivacySettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("showOnlineStatus", true);
        settings.put("allowFriendRequests", true);
        settings.put("showLastSeen", true);
        return toJsonString(settings);
    }
    
    /**
     * 创建默认通知设置
     */
    private String createDefaultNotificationSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enableNotifications", true);
        settings.put("enableSoundNotifications", true);
        settings.put("enableVibration", true);
        settings.put("loginNotifications", true);
        return toJsonString(settings);
    }
    
    /**
     * 创建默认主题设置
     */
    private String createDefaultThemeSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", "light");
        settings.put("fontSize", 14);
        settings.put("twoFactorEnabled", false);
        return toJsonString(settings);
    }
    
    /**
     * 解析JSON设置
     */
    private Map<String, Object> parseJsonSettings(String jsonSettings) {
        if (!StringUtils.hasText(jsonSettings)) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(jsonSettings, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("解析JSON设置失败: {}", jsonSettings, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 转换为JSON字符串
     */
    private String toJsonString(Map<String, Object> settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            log.error("转换JSON字符串失败", e);
            return "{}";
        }
    }
    
    /**
     * 获取布尔值
     */
    private Boolean getBooleanValue(Map<String, Object> settings, String key, Boolean defaultValue) {
        Object value = settings.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * 获取字符串值
     */
    private String getStringValue(Map<String, Object> settings, String key, String defaultValue) {
        Object value = settings.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    /**
     * 获取整数值
     */
    private Integer getIntegerValue(Map<String, Object> settings, String key, Integer defaultValue) {
        Object value = settings.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}