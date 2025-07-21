package com.im.imcommunicationsystem.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for content moderation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModerationRequest {

    /**
     * The report ID to handle
     */
    @NotNull(message = "Report ID is required")
    @Positive(message = "Report ID must be positive")
    private Long reportId;
    
    /**
     * The action to take (process, resolve, reject)
     */
    @NotBlank(message = "Action is required")
    private String action;
    
    /**
     * The result of the moderation
     */
    @NotBlank(message = "Result is required")
    private String result;
    
    /**
     * Additional notes for the moderation
     */
    private String note;
    
    /**
     * The user action to take (warn, temporary_ban, permanent_ban, none)
     */
    private String userAction;
    
    /**
     * The content action to take (delete, hide, mark_as_sensitive, none)
     */
    private String contentAction;
} 