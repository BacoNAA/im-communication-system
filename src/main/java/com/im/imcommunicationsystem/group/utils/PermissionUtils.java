package com.im.imcommunicationsystem.group.utils;

import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;

/**
 * 权限工具类
 * 提供群组权限验证和管理相关的工具方法
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class PermissionUtils {

    /**
     * 检查用户是否有权限
     * 
     * @param userRole 用户角色
     * @param requiredRole 所需角色
     * @return 是否有权限
     */
    public static boolean hasPermission(String userRole, String requiredRole) {
        if (userRole == null || requiredRole == null) {
            return false;
        }
        
        int userPriority = GroupUtils.getRolePriority(userRole);
        int requiredPriority = GroupUtils.getRolePriority(requiredRole);
        
        return userPriority >= requiredPriority;
    }

    /**
     * 检查用户是否有权限邀请成员
     * 
     * @param member 群成员
     * @return 是否有权限
     */
    public static boolean canInviteMembers(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        // 群主和管理员可以邀请成员
        return GroupUtils.isAdmin(member);
    }

    /**
     * 检查用户是否有权限移出成员
     * 
     * @param operatorMember 操作者成员信息
     * @param targetMember 目标成员信息
     * @return 是否有权限
     */
    public static boolean canRemoveMembers(GroupMember operatorMember, GroupMember targetMember) {
        if (operatorMember == null || targetMember == null) {
            return false;
        }
        
        // 不能移出自己
        if (operatorMember.getId().getUserId().equals(targetMember.getId().getUserId())) {
            return false;
        }
        
        // 群主可以移出任何人
        if (operatorMember.getRole() == GroupMemberRole.owner) {
            return true;
        }
        
        // 管理员可以移出普通成员，但不能移出群主或其他管理员
        if (operatorMember.getRole() == GroupMemberRole.admin) {
            return targetMember.getRole() == GroupMemberRole.member;
        }
        
        // 普通成员没有权限移出他人
        return false;
    }

    /**
     * 检查用户是否有权限管理管理员
     * 
     * @param member 群成员
     * @return 是否有权限
     */
    public static boolean canManageAdmins(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        // 只有群主可以管理管理员
        return member.getRole() == GroupMemberRole.owner;
    }

    /**
     * 检查用户是否有权限管理公告
     * 
     * @param member 群成员
     * @return 是否有权限
     */
    public static boolean canManageAnnouncements(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        // 群主和管理员可以管理公告
        return GroupUtils.isAdmin(member);
    }

    /**
     * 检查用户是否有权限管理禁言
     * 
     * @param operatorMember 操作者成员信息
     * @param targetMember 目标成员信息
     * @return 是否有权限
     */
    public static boolean canManageMute(GroupMember operatorMember, GroupMember targetMember) {
        if (operatorMember == null || targetMember == null) {
            return false;
        }
        
        // 不能禁言自己
        if (operatorMember.getId().getUserId().equals(targetMember.getId().getUserId())) {
            return false;
        }
        
        // 群主可以禁言任何人
        if (operatorMember.getRole() == GroupMemberRole.owner) {
            return true;
        }
        
        // 管理员可以禁言普通成员，但不能禁言群主或其他管理员
        if (operatorMember.getRole() == GroupMemberRole.admin) {
            return targetMember.getRole() == GroupMemberRole.member;
        }
        
        // 普通成员没有权限禁言他人
        return false;
    }

    /**
     * 检查用户是否有权限解散群组
     * 
     * @param member 群成员
     * @return 是否有权限
     */
    public static boolean canDissolveGroup(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        // 只有群主可以解散群组
        return member.getRole() == GroupMemberRole.owner;
    }

    /**
     * 检查用户是否有权限更新群组信息
     * 
     * @param member 群成员
     * @return 是否有权限
     */
    public static boolean canUpdateGroupInfo(GroupMember member) {
        if (member == null) {
            return false;
        }
        
        // 群主和管理员可以更新群组信息
        return GroupUtils.isAdmin(member);
    }
} 