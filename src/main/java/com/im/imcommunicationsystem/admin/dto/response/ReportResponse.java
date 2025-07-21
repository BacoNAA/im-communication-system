package com.im.imcommunicationsystem.admin.dto.response;

import com.im.imcommunicationsystem.admin.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for report responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    /**
     * The report ID
     */
    private Long reportId;
    
    /**
     * The reporter ID
     */
    private Long reporterId;
    
    /**
     * The reporter username
     */
    private String reporterUsername;
    
    /**
     * The reported user ID (if applicable)
     */
    private Long reportedUserId;
    
    /**
     * The reported user username (if applicable)
     */
    private String reportedUsername;
    
    /**
     * The reported content type
     */
    private String reportedContentType;
    
    /**
     * The reported content ID
     */
    private Long reportedContentId;
    
    /**
     * The report reason
     */
    private String reason;
    
    /**
     * The report description
     */
    private String description;
    
    /**
     * The report status
     */
    private String status;
    
    /**
     * The report creation time
     */
    private LocalDateTime createdAt;
    
    /**
     * The report handling time
     */
    private LocalDateTime handledAt;
    
    /**
     * The admin ID who handled the report
     */
    private Long handledBy;
} 