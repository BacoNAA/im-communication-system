package com.im.imcommunicationsystem.relationship.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友标签响应DTO
 */
@Data
public class ContactTagResponse {

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签所有者ID
     */
    private Long userId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 使用该标签的联系人数量
     */
    private Integer contactCount;

    /**
     * 是否为默认标签
     */
    private Boolean isDefault;

    /**
     * 标签排序
     */
    private Integer sortOrder;
}