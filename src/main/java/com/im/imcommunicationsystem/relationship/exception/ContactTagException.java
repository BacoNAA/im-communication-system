package com.im.imcommunicationsystem.relationship.exception;

/**
 * 好友标签相关异常类
 * 用于处理好友标签管理中的业务异常
 */
public class ContactTagException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 标签ID
     */
    private final Long tagId;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 标签名称
     */
    private final String tagName;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ContactTagException(String message) {
        super(message);
        this.errorCode = "TAG_ERROR";
        this.tagId = null;
        this.userId = null;
        this.tagName = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public ContactTagException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.tagId = null;
        this.userId = null;
        this.tagName = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param tagId 标签ID
     * @param userId 用户ID
     * @param tagName 标签名称
     */
    public ContactTagException(String errorCode, String message, Long tagId, Long userId, String tagName) {
        super(message);
        this.errorCode = errorCode;
        this.tagId = tagId;
        this.userId = userId;
        this.tagName = tagName;
    }

    /**
     * 标签不存在异常
     */
    public static ContactTagException tagNotFound(Long tagId) {
        return new ContactTagException(
            "TAG_NOT_FOUND",
            String.format("标签不存在: 标签ID=%d", tagId),
            tagId, null, null
        );
    }

    /**
     * 标签名称已存在异常
     */
    public static ContactTagException tagNameAlreadyExists(Long userId, String tagName) {
        return new ContactTagException(
            "TAG_NAME_ALREADY_EXISTS",
            String.format("标签名称已存在: 用户ID=%d, 标签名称=%s", userId, tagName),
            null, userId, tagName
        );
    }

    /**
     * 标签名称无效异常
     */
    public static ContactTagException invalidTagName(String tagName) {
        return new ContactTagException(
            "INVALID_TAG_NAME",
            String.format("标签名称无效: %s", tagName),
            null, null, tagName
        );
    }

    /**
     * 标签颜色无效异常
     */
    public static ContactTagException invalidTagColor(String color) {
        return new ContactTagException(
            "INVALID_TAG_COLOR",
            String.format("标签颜色无效: %s", color)
        );
    }

    /**
     * 无权限操作标签异常
     */
    public static ContactTagException noPermission(Long tagId, Long userId) {
        return new ContactTagException(
            "NO_PERMISSION",
            String.format("无权限操作该标签: 标签ID=%d, 用户ID=%d", tagId, userId),
            tagId, userId, null
        );
    }

    /**
     * 标签数量超限异常
     */
    public static ContactTagException tagLimitExceeded(Long userId, int maxTags) {
        return new ContactTagException(
            "TAG_LIMIT_EXCEEDED",
            String.format("标签数量超过限制: 用户ID=%d, 最大标签数=%d", userId, maxTags),
            null, userId, null
        );
    }

    /**
     * 标签正在使用中异常
     */
    public static ContactTagException tagInUse(Long tagId, int contactCount) {
        return new ContactTagException(
            "TAG_IN_USE",
            String.format("标签正在使用中，无法删除: 标签ID=%d, 使用联系人数=%d", tagId, contactCount),
            tagId, null, null
        );
    }

    /**
     * 默认标签不能删除异常
     */
    public static ContactTagException cannotDeleteDefaultTag(Long tagId) {
        return new ContactTagException(
            "CANNOT_DELETE_DEFAULT_TAG",
            String.format("默认标签不能删除: 标签ID=%d", tagId),
            tagId, null, null
        );
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public Long getTagId() {
        return tagId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTagName() {
        return tagName;
    }
}