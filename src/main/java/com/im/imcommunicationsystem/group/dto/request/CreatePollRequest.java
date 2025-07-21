package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建投票的请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollRequest {
    
    /**
     * 投票标题/问题
     */
    @NotBlank(message = "投票标题不能为空")
    @Size(max = 255, message = "投票标题最多255个字符")
    private String title;
    
    /**
     * 投票描述（可选）
     */
    @Size(max = 1000, message = "投票描述最多1000个字符")
    private String description;
    
    /**
     * 是否多选
     */
    @NotNull(message = "请指定是否为多选投票")
    private Boolean isMultiple;
    
    /**
     * 是否匿名投票
     */
    @NotNull(message = "请指定是否为匿名投票")
    private Boolean isAnonymous;
    
    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;
    
    /**
     * 投票选项
     */
    @NotEmpty(message = "至少需要提供两个投票选项")
    @Size(min = 2, max = 20, message = "投票选项数量必须在2到20之间")
    private List<String> options;
} 