package com.im.imcommunicationsystem.moment.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest.VisibilityRules;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可见性工具类
 */
@Slf4j
public class VisibilityUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查可见性权限
     *
     * @param moment 动态
     * @param viewerId 查看者ID
     * @return 是否有权限查看
     */
    public static boolean checkVisibilityPermission(Moment moment, Long viewerId) {
        if (moment == null || viewerId == null) {
            return false;
        }

        // 发布者自己总是可见
        if (moment.getUserId().equals(viewerId)) {
            return true;
        }

        VisibilityType visibilityType = moment.getVisibilityType();
        switch (visibilityType) {
            case PUBLIC:
                return true; // 公开可见
            case PRIVATE:
                return false; // 私密（仅自己可见）
            case CUSTOM:
                return checkCustomVisibility(moment.getVisibilityRules(), viewerId);
            default:
                return false;
        }
    }

    /**
     * 检查自定义可见性
     *
     * @param visibilityRulesJson 可见性规则JSON
     * @param viewerId 查看者ID
     * @return 是否有权限查看
     */
    private static boolean checkCustomVisibility(String visibilityRulesJson, Long viewerId) {
        try {
            if (visibilityRulesJson == null || visibilityRulesJson.isEmpty()) {
                return true; // 默认可见
            }

            VisibilityRules rules = parseVisibilityRules(visibilityRulesJson);
            
            // 如果在阻止列表中，则不可见
            if (rules.getBlockedUserIds() != null && rules.getBlockedUserIds().contains(viewerId)) {
                return false;
            }

            // 如果有允许列表，则必须在列表中才可见
            if (rules.getAllowedUserIds() != null && !rules.getAllowedUserIds().isEmpty()) {
                return rules.getAllowedUserIds().contains(viewerId);
            }

            return true; // 默认可见
        } catch (Exception e) {
            log.error("解析可见性规则失败: {}", e.getMessage());
            return false; // 解析失败默认不可见
        }
    }

    /**
     * 解析可见性规则
     *
     * @param visibilityRulesJson 可见性规则JSON
     * @return 可见性规则对象
     */
    public static VisibilityRules parseVisibilityRules(String visibilityRulesJson) {
        try {
            if (visibilityRulesJson == null || visibilityRulesJson.isEmpty()) {
                return new VisibilityRules(Collections.emptyList(), Collections.emptyList());
            }
            
            return objectMapper.readValue(visibilityRulesJson, VisibilityRules.class);
        } catch (IOException e) {
            log.error("解析可见性规则失败: {}", e.getMessage());
            return new VisibilityRules(Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * 将可见性规则转换为JSON
     *
     * @param rules 可见性规则
     * @return JSON字符串
     */
    public static String convertRulesToJson(VisibilityRules rules) {
        if (rules == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            log.error("转换可见性规则为JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据可见性过滤用户
     *
     * @param userId 当前用户ID
     * @param userIds 用户ID列表
     * @param blockedUserIds 阻止的用户ID列表
     * @return 过滤后的用户ID列表
     */
    public static List<Long> filterUsersByVisibility(Long userId, List<Long> userIds, List<Long> blockedUserIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> filteredUserIds = new ArrayList<>(userIds);
        
        // 移除阻止的用户
        if (blockedUserIds != null && !blockedUserIds.isEmpty()) {
            filteredUserIds.removeAll(blockedUserIds);
        }
        
        return filteredUserIds;
    }
} 