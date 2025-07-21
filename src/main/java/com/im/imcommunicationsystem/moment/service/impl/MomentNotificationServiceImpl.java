package com.im.imcommunicationsystem.moment.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import com.im.imcommunicationsystem.moment.service.MomentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 动态通知服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomentNotificationServiceImpl implements MomentNotificationService {

    private final MomentRepository momentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notifyMomentLike(Long momentId, Long momentOwnerId, Long likeUserId) {
        log.info("发送动态点赞通知: momentId={}, momentOwnerId={}, likeUserId={}", momentId, momentOwnerId, likeUserId);
        
        try {
            // 自己点赞自己的动态不发送通知
            if (momentOwnerId.equals(likeUserId)) {
                log.info("用户点赞自己的动态，不发送通知");
                return;
            }
            
            // 获取点赞用户信息
            User likeUser = userRepository.findById(likeUserId)
                    .orElse(null);
            
            if (likeUser == null) {
                log.error("点赞用户不存在: likeUserId={}", likeUserId);
                return;
            }
            
            // 获取动态信息
            Moment moment = momentRepository.findById(momentId)
                    .orElse(null);
                    
            if (moment == null) {
                log.error("动态不存在: momentId={}", momentId);
                return;
            }
            
            // 构建通知标题和内容
            String title = "动态点赞";
            String content = likeUser.getNickname() + "点赞了你的动态";
            
            // 发送通知
            sendMomentNotification(
                momentOwnerId,
                "MOMENT_LIKE",
                title,
                content,
                momentId,
                "MOMENT"
            );
            
            log.info("动态点赞通知发送成功");
        } catch (Exception e) {
            log.error("发送动态点赞通知失败: momentId={}, error={}", momentId, e.getMessage(), e);
        }
    }

    @Override
    public void notifyMomentComment(Long momentId, Long momentOwnerId, Long commentUserId, Long commentId, String commentContent) {
        log.info("发送动态评论通知: momentId={}, momentOwnerId={}, commentUserId={}, commentId={}", 
                momentId, momentOwnerId, commentUserId, commentId);
        
        try {
            // 自己评论自己的动态不发送通知
            if (momentOwnerId.equals(commentUserId)) {
                log.info("用户评论自己的动态，不发送通知");
                return;
            }
            
            // 获取评论用户信息
            User commentUser = userRepository.findById(commentUserId)
                    .orElse(null);
            
            if (commentUser == null) {
                log.error("评论用户不存在: commentUserId={}", commentUserId);
                return;
            }
            
            // 获取动态信息
            Moment moment = momentRepository.findById(momentId)
                    .orElse(null);
                    
            if (moment == null) {
                log.error("动态不存在: momentId={}", momentId);
                return;
            }
            
            // 构建通知标题和内容
            String title = "动态评论";
            String content = commentUser.getNickname() + "评论了你的动态: " + 
                    (commentContent != null && commentContent.length() > 20 
                    ? commentContent.substring(0, 20) + "..." 
                    : commentContent);
            
            // 发送通知
            sendMomentNotification(
                momentOwnerId,
                "MOMENT_COMMENT",
                title,
                content,
                commentId,
                "COMMENT"
            );
            
            log.info("动态评论通知发送成功");
        } catch (Exception e) {
            log.error("发送动态评论通知失败: momentId={}, commentId={}, error={}", 
                    momentId, commentId, e.getMessage(), e);
        }
    }

    @Override
    public void notifyCommentReply(Long momentId, Long commentId, Long replyId, Long commentOwnerId, Long replyUserId, String replyContent) {
        log.info("发送评论回复通知: momentId={}, commentId={}, replyId={}, commentOwnerId={}, replyUserId={}", 
                momentId, commentId, replyId, commentOwnerId, replyUserId);
        
        try {
            // 自己回复自己的评论不发送通知
            if (commentOwnerId.equals(replyUserId)) {
                log.info("用户回复自己的评论，不发送通知");
                return;
            }
            
            // 获取回复用户信息
            User replyUser = userRepository.findById(replyUserId)
                    .orElse(null);
            
            if (replyUser == null) {
                log.error("回复用户不存在: replyUserId={}", replyUserId);
                return;
            }
            
            // 构建通知标题和内容
            String title = "评论回复";
            String content = replyUser.getNickname() + "回复了你的评论: " + 
                    (replyContent != null && replyContent.length() > 20 
                    ? replyContent.substring(0, 20) + "..." 
                    : replyContent);
            
            // 发送通知
            sendMomentNotification(
                commentOwnerId,
                "COMMENT_REPLY",
                title,
                content,
                replyId,
                "REPLY"
            );
            
            log.info("评论回复通知发送成功");
        } catch (Exception e) {
            log.error("发送评论回复通知失败: momentId={}, commentId={}, replyId={}, error={}", 
                    momentId, commentId, replyId, e.getMessage(), e);
        }
    }

    @Override
    public void sendMomentNotification(Long toUserId, String type, String title, String content, Long targetId, String targetType) {
        log.info("发送动态相关通知: toUserId={}, type={}, title={}, targetId={}, targetType={}", 
                toUserId, type, title, targetId, targetType);
        
        try {
            // 这里实现发送通知的逻辑
            // 可以通过WebSocket、系统消息或事件发布等方式
            
            // 创建通知事件并发布
            MomentNotificationEvent event = new MomentNotificationEvent(
                this,
                toUserId,
                type,
                title,
                content,
                targetId,
                targetType
            );
            
            eventPublisher.publishEvent(event);
            
            log.info("动态通知事件发布成功: toUserId={}, type={}", toUserId, type);
        } catch (Exception e) {
            log.error("发送动态通知失败: toUserId={}, type={}, error={}", 
                    toUserId, type, e.getMessage(), e);
        }
    }
    
    /**
     * 动态通知事件内部类
     */
    public static class MomentNotificationEvent {
        private final Object source;
        private final Long toUserId;
        private final String type;
        private final String title;
        private final String content;
        private final Long targetId;
        private final String targetType;
        
        public MomentNotificationEvent(
                Object source, 
                Long toUserId, 
                String type, 
                String title, 
                String content, 
                Long targetId, 
                String targetType) {
            this.source = source;
            this.toUserId = toUserId;
            this.type = type;
            this.title = title;
            this.content = content;
            this.targetId = targetId;
            this.targetType = targetType;
        }

        public Object getSource() {
            return source;
        }

        public Long getToUserId() {
            return toUserId;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public Long getTargetId() {
            return targetId;
        }

        public String getTargetType() {
            return targetType;
        }
    }
} 