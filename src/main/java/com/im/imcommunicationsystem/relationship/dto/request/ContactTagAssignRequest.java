package com.im.imcommunicationsystem.relationship.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 联系人标签分配请求DTO
 */
@Data
public class ContactTagAssignRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    /**
     * 标签ID列表
     */
    @NotEmpty(message = "标签ID列表不能为空")
    private List<Long> tagIds;
}