package com.im.imcommunicationsystem.message.dto.request;

import com.im.imcommunicationsystem.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息搜索请求对象
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchRequest {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /**
     * 会话ID（会话内搜索时使用）
     */
    private Long conversationId;

    /**
     * 消息类型过滤
     */
    private List<MessageType> messageTypes;

    /**
     * 发送者ID过滤
     */
    private List<Long> senderIds;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码（从0开始）
     */
    @Min(value = 0, message = "页码不能小于0")
    @Builder.Default
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    @Builder.Default
    private Integer size = 20;

    /**
     * 是否高亮显示
     */
    @Builder.Default
    private Boolean highlight = true;

    /**
     * 排序字段（默认按时间倒序）
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * 排序方向（asc/desc）
     */
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * 验证请求参数
     * 
     * @return 验证结果
     */
    public boolean isValid() {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        if (page == null || page < 0) {
            return false;
        }
        
        if (size == null || size < 1 || size > 100) {
            return false;
        }
        
        // 检查时间范围
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查是否为会话内搜索
     * 
     * @return 是否为会话内搜索
     */
    public boolean isConversationSearch() {
        return conversationId != null;
    }

    /**
     * 检查是否为全局搜索
     * 
     * @return 是否为全局搜索
     */
    public boolean isGlobalSearch() {
        return conversationId == null;
    }

    /**
     * 检查是否有消息类型过滤
     * 
     * @return 是否有消息类型过滤
     */
    public boolean hasMessageTypeFilter() {
        return messageTypes != null && !messageTypes.isEmpty();
    }

    /**
     * 检查是否有发送者过滤
     * 
     * @return 是否有发送者过滤
     */
    public boolean hasSenderFilter() {
        return senderIds != null && !senderIds.isEmpty();
    }

    /**
     * 检查是否有时间范围过滤
     * 
     * @return 是否有时间范围过滤
     */
    public boolean hasTimeRangeFilter() {
        return startTime != null || endTime != null;
    }

    /**
     * 获取有效关键词
     * 
     * @return 有效关键词
     */
    public String getEffectiveKeyword() {
        return keyword != null ? keyword.trim() : "";
    }

    /**
     * 获取偏移量
     * 
     * @return 偏移量
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * 检查排序方向是否有效
     * 
     * @return 是否有效
     */
    public boolean isValidSortDirection() {
        return "asc".equalsIgnoreCase(sortDirection) || "desc".equalsIgnoreCase(sortDirection);
    }

    /**
     * 获取标准化的排序方向
     * 
     * @return 标准化的排序方向
     */
    public String getNormalizedSortDirection() {
        if ("asc".equalsIgnoreCase(sortDirection)) {
            return "asc";
        }
        return "desc";
    }
}