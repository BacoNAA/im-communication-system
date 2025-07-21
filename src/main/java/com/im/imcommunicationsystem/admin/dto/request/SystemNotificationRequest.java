package com.im.imcommunicationsystem.admin.dto.request;

import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for system notification operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationRequest {

    /**
     * The notification title
     */
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;
    
    /**
     * The notification content
     */
    @NotBlank(message = "Content is required")
    private String content;
    
    /**
     * The notification type
     */
    @NotBlank(message = "Type is required")
    private String type;
    
    /**
     * List of user IDs to target (null means all users)
     */
    private List<Long> targetUserIds;
} 