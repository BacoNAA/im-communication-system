package com.im.imcommunicationsystem.moment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建评论请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    
    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;
    
    /**
     * 父评论ID（可选）
     */
    private Long parentCommentId;
    
    /**
     * 回复目标用户ID（可选）
     */
    private Long replyToUserId;
    
    /**
     * 是否私密评论
     */
    private Boolean isPrivate;
} 