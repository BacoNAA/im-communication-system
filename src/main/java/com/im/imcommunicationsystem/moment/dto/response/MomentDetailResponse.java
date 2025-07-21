package com.im.imcommunicationsystem.moment.dto.response;

import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 动态详情响应DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MomentDetailResponse extends MomentResponse {
    
    /**
     * 点赞用户列表
     */
    private List<UserBriefInfo> likes;
    
    /**
     * 评论列表
     */
    private List<CommentResponse> comments;
    
    /**
     * 可见性类型
     */
    private VisibilityType visibilityType;
    
    /**
     * 用户简要信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBriefInfo {
        /**
         * 用户ID
         */
        private Long id;
        
        /**
         * 用户昵称
         */
        private String nickname;
        
        /**
         * 用户头像
         */
        private String avatar;
    }
} 