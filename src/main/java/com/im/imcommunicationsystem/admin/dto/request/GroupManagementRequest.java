package com.im.imcommunicationsystem.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for group management operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupManagementRequest {

    /**
     * The group ID to manage
     */
    @NotNull(message = "Group ID is required")
    @Positive(message = "Group ID must be positive")
    private Long groupId;
    
    /**
     * The action to perform (freeze, unfreeze)
     */
    @NotBlank(message = "Action is required")
    private String action;
    
    /**
     * The reason for the action
     */
    @NotBlank(message = "Reason is required")
    private String reason;
    
    /**
     * The duration of the action in hours (optional, for temporary actions)
     */
    private Integer duration;
} 