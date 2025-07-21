package com.im.imcommunicationsystem.moment.service.impl;

import com.im.imcommunicationsystem.moment.dto.request.CreateCommentRequest;
import com.im.imcommunicationsystem.moment.dto.response.CommentResponse;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.entity.MomentComment;
import com.im.imcommunicationsystem.moment.exception.MomentNotFoundException;
import com.im.imcommunicationsystem.moment.repository.MomentCommentRepository;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import com.im.imcommunicationsystem.moment.service.MomentCommentService;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.auth.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentCommentServiceImpl implements MomentCommentService {

    private final MomentCommentRepository commentRepository;
    private final MomentRepository momentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse createComment(Long momentId, Long userId, CreateCommentRequest request) {
        try {
            log.info("创建评论: momentId={}, userId={}, content={}", momentId, userId, 
                    request.getContent() != null ? request.getContent().substring(0, Math.min(20, request.getContent().length())) + "..." : null);
            
            // 检查参数
            if (momentId == null) {
                throw new IllegalArgumentException("动态ID不能为空");
            }
            
            if (userId == null) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            
            if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("评论内容不能为空");
            }
            
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
            // 检查用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
        // 创建评论实体
        MomentComment comment = new MomentComment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
            comment.setContent(request.getContent().trim());
        
        // 设置是否为私密评论
        comment.setIsPrivate(request.getIsPrivate() != null && request.getIsPrivate());
            
            // 初始化可能为null的字段
            comment.setParentCommentId(null);
            comment.setReplyToUserId(null);
        
        // 保存评论
            log.debug("保存评论实体到数据库");
        MomentComment savedComment = commentRepository.save(comment);
        
        // 更新动态评论计数
            log.debug("更新动态评论计数: momentId={}, 当前评论数={}", momentId, moment.getCommentCount());
            moment.setCommentCount(moment.getCommentCount() != null ? moment.getCommentCount() + 1 : 1);
        momentRepository.save(moment);
        
        // 返回评论响应
            log.info("评论创建成功: commentId={}", savedComment.getId());
        return convertToCommentResponse(savedComment);
        } catch (Exception e) {
            log.error("创建评论失败: momentId={}, userId={}, error={}", momentId, userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<CommentResponse> getMomentComments(Long momentId, Long currentUserId, Pageable pageable) {
        // 检查动态是否存在
        if (!momentRepository.existsById(momentId)) {
            throw new MomentNotFoundException("动态不存在");
        }
        
        // 获取动态作者ID，用于判断私密评论可见性
        Long momentOwnerId = momentRepository.findById(momentId)
                .map(Moment::getUserId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 获取评论分页 - 注意：仓库方法是按升序，与接口说明一致
        Page<MomentComment> comments = commentRepository.findByMomentIdAndParentCommentIdIsNullOrderByCreatedAtAsc(momentId, pageable);
        
        // 根据当前用户身份过滤私密评论
        boolean isMomentOwner = momentOwnerId.equals(currentUserId);
        
        List<CommentResponse> filteredComments = comments.getContent().stream()
            .filter(comment -> {
                // 如果是私密评论且当前用户不是动态发布者也不是评论作者，则不可见
                if (comment.getIsPrivate() != null && comment.getIsPrivate() 
                        && !isMomentOwner && !comment.getUserId().equals(currentUserId)) {
                    return false;
                }
                return true;
            })
            .map(this::convertToCommentResponse)
            .collect(Collectors.toList());
        
        return new PageImpl<>(filteredComments, pageable, comments.getTotalElements());
    }

    @Override
    public List<CommentResponse> getMomentCommentsWithReplies(Long momentId, Long currentUserId) {
        // 检查动态是否存在
        if (!momentRepository.existsById(momentId)) {
            throw new MomentNotFoundException("动态不存在");
        }
        
        // 获取动态作者ID，用于判断私密评论可见性
        Long momentOwnerId = momentRepository.findById(momentId)
                .map(Moment::getUserId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
                
        boolean isMomentOwner = momentOwnerId.equals(currentUserId);
        
        // 获取所有一级评论（没有分页参数的调用需要获取全部，使用JPA查询）
        List<MomentComment> allComments = commentRepository.findAllCommentsAndRepliesByMomentId(momentId);
        
        // 构建评论树结构
        Map<Long, CommentResponse> commentMap = new HashMap<>();
        List<CommentResponse> rootComments = new ArrayList<>();
        
        // 转换所有评论为响应对象，并过滤私密评论
        for (MomentComment comment : allComments) {
            // 如果是私密评论且当前用户不是动态发布者也不是评论作者，则跳过
            if (comment.getIsPrivate() != null && comment.getIsPrivate() 
                    && !isMomentOwner && !comment.getUserId().equals(currentUserId)) {
                continue;
            }
            
            CommentResponse response = convertToCommentResponse(comment);
            commentMap.put(response.getId(), response);
            
            if (comment.getParentCommentId() == null) {
                // 根评论
                response.setReplies(new ArrayList<>());
                rootComments.add(response);
            } else {
                // 回复评论
                CommentResponse parentResponse = commentMap.get(comment.getParentCommentId());
                if (parentResponse != null) {
                    if (parentResponse.getReplies() == null) {
                        parentResponse.setReplies(new ArrayList<>());
                    }
                    parentResponse.getReplies().add(response);
                }
            }
        }
        
        return rootComments;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        MomentComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 检查是否是评论作者
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除该评论");
        }
        
        // 获取所有回复（注意使用正确的方法签名）
        Page<MomentComment> repliesPage = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId, PageRequest.of(0, Integer.MAX_VALUE));
        List<MomentComment> replies = repliesPage.getContent();
        
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }
        
        // 删除评论
        commentRepository.delete(comment);
        
        // 更新动态评论计数
        Moment moment = momentRepository.findById(comment.getMomentId())
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 减去删除的评论数（1 + 所有回复）
        int deletedCommentsCount = 1 + replies.size();
        moment.setCommentCount(Math.max(0, moment.getCommentCount() - deletedCommentsCount));
        momentRepository.save(moment);
        
        return true;
    }

    @Override
    @Transactional
    public CommentResponse replyToComment(Long momentId, Long commentId, Long userId, CreateCommentRequest request) {
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 检查父评论是否存在
        MomentComment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("被回复的评论不存在"));
        
        // 创建回复评论
        MomentComment reply = new MomentComment();
        reply.setMomentId(momentId);
        reply.setUserId(userId);
        reply.setContent(request.getContent());
        reply.setParentCommentId(commentId);
        
        // 设置回复目标用户ID（可能是父评论的作者或其他指定用户）
        Long replyToUserId = request.getReplyToUserId() != null ? 
                request.getReplyToUserId() : parentComment.getUserId();
        reply.setReplyToUserId(replyToUserId);
        
        // 设置是否为私密评论
        reply.setIsPrivate(request.getIsPrivate() != null && request.getIsPrivate());
        
        // 保存评论
        MomentComment savedReply = commentRepository.save(reply);
        
        // 更新动态评论计数
        moment.setCommentCount(moment.getCommentCount() + 1);
        momentRepository.save(moment);
        
        // 返回评论响应
        return convertToCommentResponse(savedReply);
    }

    @Override
    public long getCommentCount(Long momentId) {
        return commentRepository.countByMomentId(momentId);
    }
    
    // 辅助方法：将评论实体转换为响应对象
    private CommentResponse convertToCommentResponse(MomentComment comment) {
        CommentResponse response = CommentResponse.builder()
            .id(comment.getId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .parentCommentId(comment.getParentCommentId())
            .replyToUserId(comment.getReplyToUserId())
            .isPrivate(comment.getIsPrivate())
            .createdAt(comment.getCreatedAt())
            .build();
        
        // 获取用户信息
        Optional<User> user = userRepository.findById(comment.getUserId());
        user.ifPresent(u -> {
            response.setUserNickname(u.getNickname());
            response.setUserAvatar(u.getAvatarUrl());
        });
        
        // 如果有回复目标用户，添加其信息
        if (comment.getReplyToUserId() != null) {
            Optional<User> replyToUser = userRepository.findById(comment.getReplyToUserId());
            replyToUser.ifPresent(u -> response.setReplyToUserNickname(u.getNickname()));
        }
        
        return response;
    }
} 