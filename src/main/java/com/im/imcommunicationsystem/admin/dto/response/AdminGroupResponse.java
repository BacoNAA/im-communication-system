package com.im.imcommunicationsystem.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for group management responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGroupResponse {

    /**
     * The group ID
     */
    private Long groupId;
    
    /**
     * The group name
     */
    private String groupName;
    
    /**
     * The number of members
     */
    private Integer memberCount;
    
    /**
     * The group status (active, frozen, etc.)
     */
    private String status;
    
    /**
     * The creation date
     */
    private LocalDateTime createdAt;
    
    /**
     * The owner ID
     */
    private Long ownerId;
    
    /**
     * The owner username
     */
    private String ownerUsername;
    
    /**
     * The group avatar URL
     */
    private String avatarUrl;
    
    /**
     * The group description
     */
    private String description;
    
    /**
     * The reason for freeze (if applicable)
     */
    private String freezeReason;
    
    /**
     * The freeze end date (if temporary)
     */
    private LocalDateTime freezeEndDate;
} 