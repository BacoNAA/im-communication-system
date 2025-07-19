package com.im.imcommunicationsystem.group.exception;

/**
 * 群组权限相关异常
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class GroupPermissionException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public GroupPermissionException(String message) {
        this(403, message);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public GroupPermissionException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupPermissionException(String message, Throwable cause) {
        this(403, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupPermissionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 权限不足异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param requiredRole 所需角色
     * @return 异常实例
     */
    public static GroupPermissionException insufficientPermission(Long userId, Long groupId, String requiredRole) {
        return new GroupPermissionException("用户[" + userId + "]没有群组[" + groupId + "]的" + requiredRole + "权限");
    }

    /**
     * 非群成员操作异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupPermissionException notGroupMember(Long userId, Long groupId) {
        return new GroupPermissionException("用户[" + userId + "]不是群组[" + groupId + "]的成员");
    }

    /**
     * 角色权限冲突异常
     * 
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupPermissionException roleConflict(Long userId, Long targetUserId, Long groupId) {
        return new GroupPermissionException("用户[" + userId + "]无法对群组[" + groupId + "]中的用户[" + targetUserId + "]执行操作");
    }

    /**
     * 操作被禁止异常
     * 
     * @param operation 操作名称
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupPermissionException operationForbidden(String operation, Long groupId) {
        return new GroupPermissionException("在群组[" + groupId + "]中禁止执行[" + operation + "]操作");
    }

    /**
     * 群主无法退出异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupPermissionException ownerCannotLeave(Long userId, Long groupId) {
        return new GroupPermissionException("群主[" + userId + "]无法直接退出群组[" + groupId + "]，请先转让群主权限");
    }

    /**
     * 无法操作自己异常
     * 
     * @param userId 用户ID
     * @param operation 操作名称
     * @return 异常实例
     */
    public static GroupPermissionException cannotOperateOnSelf(Long userId, String operation) {
        return new GroupPermissionException("用户[" + userId + "]不能对自己执行[" + operation + "]操作");
    }
} 