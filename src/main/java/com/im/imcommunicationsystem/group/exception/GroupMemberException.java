package com.im.imcommunicationsystem.group.exception;

/**
 * 群成员操作相关异常
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class GroupMemberException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public GroupMemberException(String message) {
        this(400, message);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public GroupMemberException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupMemberException(String message, Throwable cause) {
        this(400, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupMemberException(int code, String message, Throwable cause) {
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
     * 用户已在群中异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupMemberException userAlreadyInGroup(Long userId, Long groupId) {
        return new GroupMemberException("用户[" + userId + "]已经是群组[" + groupId + "]的成员");
    }

    /**
     * 用户不在群中异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupMemberException userNotInGroup(Long userId, Long groupId) {
        return new GroupMemberException(404, "用户[" + userId + "]不是群组[" + groupId + "]的成员");
    }

    /**
     * 邀请码无效异常
     * 
     * @param inviteCode 邀请码
     * @return 异常实例
     */
    public static GroupMemberException invalidInviteCode(String inviteCode) {
        return new GroupMemberException("邀请码[" + inviteCode + "]无效或已过期");
    }

    /**
     * 申请已存在异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupMemberException requestAlreadyExists(Long userId, Long groupId) {
        return new GroupMemberException("用户[" + userId + "]已经提交了加入群组[" + groupId + "]的申请");
    }

    /**
     * 禁言操作失败异常
     * 
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param reason 原因
     * @return 异常实例
     */
    public static GroupMemberException muteOperationFailed(Long userId, Long groupId, String reason) {
        return new GroupMemberException("对用户[" + userId + "]在群组[" + groupId + "]中的禁言操作失败: " + reason);
    }

    /**
     * 转让群主失败异常
     * 
     * @param currentOwnerId 当前群主ID
     * @param newOwnerId 新群主ID
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupMemberException transferOwnershipFailed(Long currentOwnerId, Long newOwnerId, Long groupId) {
        return new GroupMemberException("群主[" + currentOwnerId + "]转让群组[" + groupId + "]给用户[" + newOwnerId + "]失败");
    }
} 