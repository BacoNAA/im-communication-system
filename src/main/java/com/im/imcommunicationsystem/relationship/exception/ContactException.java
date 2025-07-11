package com.im.imcommunicationsystem.relationship.exception;

/**
 * 联系人相关异常类
 * 用于处理联系人关系管理中的业务异常
 */
public class ContactException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 好友ID
     */
    private final Long friendId;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ContactException(String message) {
        super(message);
        this.errorCode = "CONTACT_ERROR";
        this.userId = null;
        this.friendId = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public ContactException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userId = null;
        this.friendId = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    public ContactException(String errorCode, String message, Long userId, Long friendId) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
        this.friendId = friendId;
    }

    /**
     * 联系人不存在异常
     */
    public static ContactException contactNotFound(Long userId, Long friendId) {
        return new ContactException(
            "CONTACT_NOT_FOUND",
            String.format("联系人关系不存在: 用户ID=%d, 好友ID=%d", userId, friendId),
            userId, friendId
        );
    }

    /**
     * 联系人已存在异常
     */
    public static ContactException contactAlreadyExists(Long userId, Long friendId) {
        return new ContactException(
            "CONTACT_ALREADY_EXISTS",
            String.format("联系人关系已存在: 用户ID=%d, 好友ID=%d", userId, friendId),
            userId, friendId
        );
    }

    /**
     * 不能添加自己为好友异常
     */
    public static ContactException cannotAddSelf(Long userId) {
        return new ContactException(
            "CANNOT_ADD_SELF",
            "不能添加自己为好友",
            userId, userId
        );
    }

    /**
     * 联系人已被屏蔽异常
     */
    public static ContactException contactBlocked(Long userId, Long friendId) {
        return new ContactException(
            "CONTACT_BLOCKED",
            String.format("联系人已被屏蔽: 用户ID=%d, 好友ID=%d", userId, friendId),
            userId, friendId
        );
    }

    /**
     * 无权限操作异常
     */
    public static ContactException noPermission(Long userId, Long friendId) {
        return new ContactException(
            "NO_PERMISSION",
            String.format("无权限操作该联系人: 用户ID=%d, 好友ID=%d", userId, friendId),
            userId, friendId
        );
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFriendId() {
        return friendId;
    }
}