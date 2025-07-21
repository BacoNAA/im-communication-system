package com.im.imcommunicationsystem.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user management operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementRequest {

    /**
     * The user ID to manage
     */
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;
    
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