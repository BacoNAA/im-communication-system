package com.im.imcommunicationsystem.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user management responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    /**
     * The user ID
     */
    private Long userId;
    
    /**
     * The username
     */
    private String username;
    
    /**
     * The email
     */
    private String email;
    
    /**
     * The user status (active, frozen, etc.)
     */
    private String status;
    
    /**
     * Whether the user is active
     */
    private Boolean isActive;
    
    /**
     * The registration date
     */
    private LocalDateTime registeredAt;
    
    /**
     * The last login date
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * The number of devices
     */
    private Integer deviceCount;
    
    /**
     * The custom user ID string
     */
    private String userIdStr;
    
    /**
     * The avatar URL
     */
    private String avatarUrl;
    
    /**
     * The reason for freeze (if applicable)
     */
    private String freezeReason;
    
    /**
     * The freeze end date (if temporary)
     */
    private LocalDateTime freezeEndDate;
} 