package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投票者信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoterResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 投票时间
     */
    private String votedAt;
} 