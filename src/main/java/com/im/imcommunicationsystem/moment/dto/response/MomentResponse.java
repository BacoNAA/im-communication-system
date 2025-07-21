package com.im.imcommunicationsystem.moment.dto.response;

import com.im.imcommunicationsystem.moment.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态响应DTO
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MomentResponse {
    
    /**
     * 动态ID
     */
    private Long id;
    
    /**
     * 发布用户ID
     */
    private Long userId;
    
    /**
     * 发布用户昵称
     */
    private String userNickname;
    
    /**
     * 发布用户头像
     */
    private String userAvatar;
    
    /**
     * 动态内容
     */
    private String content;
    
    /**
     * 媒体文件URL列表
     */
    private List<String> mediaUrls;
    
    /**
     * 媒体类型
     */
    private MediaType mediaType;
    
    /**
     * 点赞数量
     */
    private Integer likeCount;
    
    /**
     * 评论数量
     */
    private Integer commentCount;
    
    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;
    
    /**
     * 发布时间
     */
    private LocalDateTime createdAt;
} 