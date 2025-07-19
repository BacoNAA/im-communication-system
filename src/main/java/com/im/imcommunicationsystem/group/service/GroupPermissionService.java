package com.im.imcommunicationsystem.group.service;

/**
 * 群组权限服务接口
 * 定义群组权限控制和验证相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface GroupPermissionService {

    /**
     * 验证用户是否有权限操作群组
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param requiredRole 所需角色（OWNER/ADMIN/MEMBER）
     * @return 是否有权限
     */
    boolean validatePermission(Long groupId, Long userId, String requiredRole);

    /**
     * 验证用户是否有权限邀请成员
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean canInviteMembers(Long groupId, Long userId);

    /**
     * 验证用户是否有权限移除成员
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    boolean canRemoveMember(Long groupId, Long userId, Long targetUserId);

    /**
     * 验证用户是否有权限管理管理员
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean canManageAdmins(Long groupId, Long userId);

    /**
     * 验证用户是否有权限管理公告
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean canManageAnnouncements(Long groupId, Long userId);

    /**
     * 验证用户是否有权限管理禁言
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    boolean canManageMute(Long groupId, Long userId, Long targetUserId);

    /**
     * 验证用户是否有权限解散群组
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean canDissolveGroup(Long groupId, Long userId);

    /**
     * 验证用户是否有权限更新群组信息
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean canUpdateGroupInfo(Long groupId, Long userId);

    /**
     * 获取用户在群组中的角色
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 角色（OWNER/ADMIN/MEMBER），不在群组中返回null
     */
    String getUserRole(Long groupId, Long userId);
} 