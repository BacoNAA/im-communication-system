package com.im.imcommunicationsystem.moment.service;

/**
 * 动态通知服务接口
 */
public interface MomentNotificationService {

    /**
     * 通知动态被点赞
     *
     * @param momentId 动态ID
     * @param momentOwnerId 动态所有者ID
     * @param likeUserId 点赞用户ID
     */
    void notifyMomentLike(Long momentId, Long momentOwnerId, Long likeUserId);

    /**
     * 通知动态被评论
     *
     * @param momentId 动态ID
     * @param momentOwnerId 动态所有者ID
     * @param commentUserId 评论用户ID
     * @param commentId 评论ID
     * @param commentContent 评论内容摘要
     */
    void notifyMomentComment(Long momentId, Long momentOwnerId, Long commentUserId, Long commentId, String commentContent);

    /**
     * 通知评论被回复
     *
     * @param momentId 动态ID
     * @param commentId 评论ID
     * @param replyId 回复ID
     * @param commentOwnerId 评论所有者ID
     * @param replyUserId 回复用户ID
     * @param replyContent 回复内容摘要
     */
    void notifyCommentReply(Long momentId, Long commentId, Long replyId, Long commentOwnerId, Long replyUserId, String replyContent);

    /**
     * 发送动态通知
     *
     * @param toUserId 接收通知的用户ID
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param targetId 目标ID
     * @param targetType 目标类型
     */
    void sendMomentNotification(Long toUserId, String type, String title, String content, Long targetId, String targetType);
} 