package com.im.imcommunicationsystem.message.exception;

/**
 * 会话相关异常类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class ConversationException extends MessageException {

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ConversationException(String message) {
        super("CONVERSATION_ERROR", message);
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public ConversationException(String message, Throwable cause) {
        super("CONVERSATION_ERROR", message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ConversationException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public ConversationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 会话不存在异常
     * 
     * @param conversationId 会话ID
     * @return 异常实例
     */
    public static ConversationException notFound(Long conversationId) {
        return new ConversationException("CONVERSATION_NOT_FOUND", 
                                       String.format("会话不存在: %d", conversationId));
    }

    /**
     * 会话已删除异常
     * 
     * @param conversationId 会话ID
     * @return 异常实例
     */
    public static ConversationException deleted(Long conversationId) {
        return new ConversationException("CONVERSATION_DELETED", 
                                       String.format("会话已删除: %d", conversationId));
    }

    /**
     * 无权限访问会话异常
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 异常实例
     */
    public static ConversationException accessDenied(Long conversationId, Long userId) {
        return new ConversationException("CONVERSATION_ACCESS_DENIED", 
                                       String.format("用户 %d 无权限访问会话 %d", userId, conversationId));
    }

    /**
     * 会话参与者数量超限异常
     * 
     * @param maxParticipants 最大参与者数量
     * @return 异常实例
     */
    public static ConversationException participantLimitExceeded(int maxParticipants) {
        return new ConversationException("CONVERSATION_PARTICIPANT_LIMIT_EXCEEDED", 
                                       String.format("会话参与者数量不能超过 %d 人", maxParticipants));
    }

    /**
     * 用户不在会话中异常
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 异常实例
     */
    public static ConversationException userNotInConversation(Long userId, Long conversationId) {
        return new ConversationException("USER_NOT_IN_CONVERSATION", 
                                       String.format("用户 %d 不在会话 %d 中", userId, conversationId));
    }

    /**
     * 用户已在会话中异常
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 异常实例
     */
    public static ConversationException userAlreadyInConversation(Long userId, Long conversationId) {
        return new ConversationException("USER_ALREADY_IN_CONVERSATION", 
                                       String.format("用户 %d 已在会话 %d 中", userId, conversationId));
    }

    /**
     * 无权限操作异常
     * 
     * @param operation 操作名称
     * @return 异常实例
     */
    public static ConversationException operationNotAllowed(String operation) {
        return new ConversationException("CONVERSATION_OPERATION_NOT_ALLOWED", 
                                       String.format("无权限执行操作: %s", operation));
    }

    /**
     * 会话类型不支持操作异常
     * 
     * @param operation 操作名称
     * @param conversationType 会话类型
     * @return 异常实例
     */
    public static ConversationException operationNotSupportedForType(String operation, String conversationType) {
        return new ConversationException("CONVERSATION_OPERATION_NOT_SUPPORTED", 
                                       String.format("%s 类型的会话不支持 %s 操作", conversationType, operation));
    }

    /**
     * 会话名称重复异常
     * 
     * @param name 会话名称
     * @return 异常实例
     */
    public static ConversationException nameAlreadyExists(String name) {
        return new ConversationException("CONVERSATION_NAME_ALREADY_EXISTS", 
                                       String.format("会话名称已存在: %s", name));
    }

    /**
     * 私聊会话已存在异常
     * 
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 异常实例
     */
    public static ConversationException privateConversationAlreadyExists(Long userId1, Long userId2) {
        return new ConversationException("PRIVATE_CONVERSATION_ALREADY_EXISTS", 
                                       String.format("用户 %d 和 %d 之间的私聊会话已存在", userId1, userId2));
    }
}