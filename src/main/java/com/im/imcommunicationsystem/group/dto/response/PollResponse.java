package com.im.imcommunicationsystem.group.dto.response;

import com.im.imcommunicationsystem.group.enums.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投票信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {
    
    /**
     * 投票ID
     */
    private Long id;
    
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 创建者ID
     */
    private Long creatorId;
    
    /**
     * 创建者昵称
     */
    private String creatorNickname;
    
    /**
     * 创建者头像URL
     */
    private String creatorAvatarUrl;
    
    /**
     * 投票标题/问题
     */
    private String title;
    
    /**
     * 投票描述（可选）
     */
    private String description;
    
    /**
     * 是否多选
     */
    private Boolean isMultiple;
    
    /**
     * 是否匿名投票
     */
    private Boolean isAnonymous;
    
    /**
     * 投票状态
     */
    private PollStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;
    
    /**
     * 总投票人数
     */
    private Long totalVoters;
    
    /**
     * 当前用户是否已投票
     */
    private Boolean hasVoted;
    
    /**
     * 投票选项列表
     */
    private List<PollOptionResponse> options;
} 