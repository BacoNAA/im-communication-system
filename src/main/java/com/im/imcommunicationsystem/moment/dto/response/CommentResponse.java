package com.im.imcommunicationsystem.moment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    /**
     * 评论ID
     */
    private Long id;
    
    /**
     * 评论用户ID
     */
    private Long userId;
    
    /**
     * 评论用户昵称
     */
    private String userNickname;
    
    /**
     * 评论用户头像
     */
    private String userAvatar;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 父评论ID
     */
    private Long parentCommentId;
    
    /**
     * 回复目标用户ID
     */
    private Long replyToUserId;
    
    /**
     * 回复目标用户昵称
     */
    private String replyToUserNickname;
    
    /**
     * 是否私密评论
     */
    private Boolean isPrivate;
    
    /**
     * 评论时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 回复列表
     */
    private List<CommentResponse> replies;
} 