package com.im.imcommunicationsystem.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建举报请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    /**
     * 被举报用户ID（可选，如果举报的是用户）
     */
    private Long reportedUserId;
    
    /**
     * 被举报内容类型（必填）
     * 例如：USER, MESSAGE, GROUP, MOMENT等
     */
    @NotBlank(message = "举报内容类型不能为空")
    private String reportedContentType;
    
    /**
     * 被举报内容ID（必填）
     */
    @NotNull(message = "举报内容ID不能为空")
    @Positive(message = "举报内容ID必须为正数")
    private Long reportedContentId;
    
    /**
     * 举报原因（必填）
     * 例如：垃圾信息、色情内容、暴力内容、诈骗信息等
     */
    @NotBlank(message = "举报原因不能为空")
    private String reason;
    
    /**
     * 举报详细描述（可选）
     */
    private String description;
} 