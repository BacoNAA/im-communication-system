package com.im.imcommunicationsystem.relationship.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人标签分配响应DTO
 */
@Data
public class ContactTagAssignmentResponse {

    /**
     * 分配记录ID
     */
    private Long assignmentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签颜色
     */
    private String tagColor;

    /**
     * 分配时间
     */
    private LocalDateTime createdAt;
}