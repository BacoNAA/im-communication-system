package com.im.imcommunicationsystem.admin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.admin.entity.AdminUser;
import com.im.imcommunicationsystem.admin.exception.AdminPermissionException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for admin permissions
 */
@Component
public class AdminPermissionUtils {

    private final ObjectMapper objectMapper;

    public AdminPermissionUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Check if the admin has user management permission
     *
     * @param adminId the admin id
     * @return true if the admin has user management permission
     */
    public boolean hasUserManagementPermission(Long adminId) {
        // For now, return true for all admin IDs
        // In a real implementation, this would check the admin's permissions
        return true;
    }

    /**
     * Check if the admin has group management permission
     *
     * @param adminId the admin id
     * @return true if the admin has group management permission
     */
    public boolean hasGroupManagementPermission(Long adminId) {
        // For now, return true for all admin IDs
        // In a real implementation, this would check the admin's permissions
        return true;
    }

    /**
     * Check if the admin has content moderation permission
     *
     * @param adminId the admin id
     * @return true if the admin has content moderation permission
     */
    public boolean hasContentModerationPermission(Long adminId) {
        // For now, return true for all admin IDs
        // In a real implementation, this would check the admin's permissions
        return true;
    }

    /**
     * Check if the admin has system notification permission
     *
     * @param adminId the admin id
     * @return true if the admin has system notification permission
     */
    public boolean hasSystemNotificationPermission(Long adminId) {
        // For now, return true for all admin IDs
        // In a real implementation, this would check the admin's permissions
        return true;
    }

    /**
     * Get the admin's permissions
     *
     * @param admin the admin user
     * @return map of permissions
     */
    public Map<String, Boolean> getAdminPermissions(AdminUser admin) {
        // In a real implementation, this would parse the admin's permissions from the database
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("USER_MANAGEMENT", true);
        permissions.put("GROUP_MANAGEMENT", true);
        permissions.put("CONTENT_MODERATION", true);
        permissions.put("SYSTEM_NOTIFICATION", true);
        
        return permissions;
    }

    /**
     * Parse the admin's permissions from JSON
     *
     * @param permissionsJson the permissions JSON
     * @return map of permissions
     */
    public Map<String, Boolean> parsePermissions(String permissionsJson) {
        try {
            if (permissionsJson == null || permissionsJson.isEmpty()) {
                return new HashMap<>();
            }
            
            return objectMapper.readValue(permissionsJson, 
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Boolean.class));
        } catch (JsonProcessingException e) {
            throw new AdminPermissionException("Failed to parse admin permissions: " + e.getMessage(), e);
        }
    }
} 