package com.im.imcommunicationsystem.group.exception;

/**
 * 群组操作相关异常
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class GroupException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public GroupException(String message) {
        this(500, message);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public GroupException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupException(String message, Throwable cause) {
        this(500, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public GroupException(int code, String message, Throwable cause) {
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
     * 群组不存在异常
     * 
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupException groupNotFound(Long groupId) {
        return new GroupException(404, "群组不存在: " + groupId);
    }

    /**
     * 群组已解散异常
     * 
     * @param groupId 群组ID
     * @return 异常实例
     */
    public static GroupException groupDissolved(Long groupId) {
        return new GroupException(410, "群组已解散: " + groupId);
    }

    /**
     * 群组名称重复异常
     * 
     * @param groupName 群组名称
     * @return 异常实例
     */
    public static GroupException duplicateGroupName(String groupName) {
        return new GroupException(400, "群组名称已存在: " + groupName);
    }

    /**
     * 群组成员已满异常
     * 
     * @param groupId 群组ID
     * @param maxMembers 最大成员数
     * @return 异常实例
     */
    public static GroupException groupMembersFull(Long groupId, int maxMembers) {
        return new GroupException(400, "群组成员已达上限(" + maxMembers + "): " + groupId);
    }

    /**
     * 群组数量超限异常
     * 
     * @param userId 用户ID
     * @param maxGroups 最大群组数
     * @return 异常实例
     */
    public static GroupException tooManyGroups(Long userId, int maxGroups) {
        return new GroupException(400, "用户创建的群组数量已达上限(" + maxGroups + "): " + userId);
    }

    /**
     * 群组名称无效异常
     * 
     * @param groupName 群组名称
     * @return 异常实例
     */
    public static GroupException invalidGroupName(String groupName) {
        return new GroupException(400, "群组名称无效: " + groupName);
    }
} 