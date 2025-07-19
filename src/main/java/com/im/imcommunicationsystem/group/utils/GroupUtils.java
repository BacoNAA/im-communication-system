package com.im.imcommunicationsystem.group.utils;

import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 群组工具类
 * 提供群组相关的通用工具方法
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Component
public class GroupUtils {

    /**
     * 群名称最大长度
     */
    public static final int MAX_GROUP_NAME_LENGTH = 100;
    
    /**
     * 群介绍最大长度
     */
    public static final int MAX_GROUP_DESCRIPTION_LENGTH = 500;
    
    /**
     * 默认最大成员数
     */
    public static final int DEFAULT_MAX_MEMBERS = 500;
    
    /**
     * 群名称正则表达式
     */
    private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5\\s]{1,100}$");

    /**
     * 生成默认群名称
     * 根据成员用户名列表生成群名称
     * 
     * @param memberNames 成员用户名列表
     * @param maxLength 最大长度
     * @return 群名称
     */
    public static String generateGroupName(List<String> memberNames, int maxLength) {
        if (memberNames == null || memberNames.isEmpty()) {
            return "新建群聊";
        }
        
        String names = memberNames.stream()
                .limit(3)
                .collect(Collectors.joining("、"));
                
        // 如果成员超过3人，添加"等"后缀
        if (memberNames.size() > 3) {
            names += "等";
        }
        
        // 添加"的群聊"后缀
        String groupName = names + "的群聊";
        
        // 确保不超过最大长度
        return truncate(groupName, maxLength);
    }

    /**
     * 截断字符串到指定长度
     * 
     * @param str 原字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength);
    }

    /**
     * 验证群名称格式
     * 
     * @param name 群名称
     * @return 是否有效
     */
    public static boolean validateGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        return GROUP_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * 格式化成员数量显示
     * 
     * @param count 成员数量
     * @return 格式化后的字符串
     */
    public static String formatMemberCount(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 10000) {
            return String.format("%.1fk", count / 1000.0);
        } else {
            return String.format("%.1fw", count / 10000.0);
        }
    }

    /**
     * 检查群组数量限制
     * 
     * @param currentCount 当前数量
     * @param maxCount 最大数量
     * @return 是否达到限制
     */
    public static boolean checkGroupLimit(int currentCount, int maxCount) {
        return currentCount < maxCount;
    }

    /**
     * 获取角色优先级
     * 用于比较角色权限高低
     * 
     * @param role 角色
     * @return 优先级（数值越大权限越高）
     */
    public static int getRolePriority(String role) {
        if (role == null) {
            return -1;
        }
        
        switch (role.toUpperCase()) {
            case "OWNER":
                return 3;
            case "ADMIN":
                return 2;
            case "MEMBER":
                return 1;
            default:
                return 0;
        }
    }

    /**
     * 比较两个角色的权限
     * 
     * @param role1 角色1
     * @param role2 角色2
     * @return 1表示role1权限更高，-1表示role2权限更高，0表示相同
     */
    public static int compareRoles(String role1, String role2) {
        int priority1 = getRolePriority(role1);
        int priority2 = getRolePriority(role2);
        
        return Integer.compare(priority1, priority2);
    }

    /**
     * 检查是否为群管理员
     * 
     * @param member 群成员
     * @return 是否为管理员
     */
    public static boolean isAdmin(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        return member.getRole() == GroupMemberRole.owner || member.getRole() == GroupMemberRole.admin;
    }
} 