package com.im.imcommunicationsystem.moment.service;

import com.im.imcommunicationsystem.moment.dto.request.CreateCommentRequest;
import com.im.imcommunicationsystem.moment.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 动态评论服务接口
 */
public interface MomentCommentService {

    /**
     * 创建评论
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @param request 创建评论请求
     * @return 创建的评论
     */
    CommentResponse createComment(Long momentId, Long userId, CreateCommentRequest request);

    /**
     * 获取动态评论列表
     *
     * @param momentId 动态ID
     * @param currentUserId 当前用户ID
     * @param pageable 分页信息
     * @return 评论分页数据
     */
    Page<CommentResponse> getMomentComments(Long momentId, Long currentUserId, Pageable pageable);

    /**
     * 获取一级评论及其回复
     *
     * @param momentId 动态ID
     * @param currentUserId 当前用户ID
     * @return 评论列表（包含回复）
     */
    List<CommentResponse> getMomentCommentsWithReplies(Long momentId, Long currentUserId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);

    /**
     * 回复评论
     *
     * @param momentId 动态ID
     * @param commentId 评论ID
     * @param userId 用户ID
     * @param request 创建评论请求
     * @return 创建的回复
     */
    CommentResponse replyToComment(Long momentId, Long commentId, Long userId, CreateCommentRequest request);

    /**
     * 获取评论数量
     *
     * @param momentId 动态ID
     * @return 评论数量
     */
    long getCommentCount(Long momentId);
} 