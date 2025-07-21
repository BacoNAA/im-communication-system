package com.im.imcommunicationsystem.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 投票选项响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionResponse {
    
    /**
     * 选项ID
     */
    private Long id;
    
    /**
     * 选项文本
     */
    private String optionText;
    
    /**
     * 投票数量
     */
    private Long voteCount;
    
    /**
     * 投票比例（百分比）
     */
    private Double percentage;
    
    /**
     * 当前用户是否选择了该选项
     */
    private Boolean isSelected;
    
    /**
     * 如果不是匿名投票，包含投票用户列表
     */
    private List<VoterResponse> voters;
} 