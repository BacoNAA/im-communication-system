package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 参与投票的请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotePollRequest {
    
    /**
     * 投票选项ID列表（单选只有一个元素，多选可以有多个）
     */
    @NotEmpty(message = "请至少选择一个选项")
    @Size(min = 1, message = "请至少选择一个选项")
    private List<Long> optionIds;
} 