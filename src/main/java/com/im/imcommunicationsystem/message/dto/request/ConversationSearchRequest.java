package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话内搜索请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSearchRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 200, message = "搜索关键词不能超过200个字符")
    private String keyword;

    /**
     * 消息类型（text, image, video, audio, file等）
     */
    private List<String> messageTypes;

    /**
     * 发送者ID列表
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
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 50, message = "每页大小不能超过50")
    private Integer size = 20;

    /**
     * 排序字段
     */
    private String sortBy = "sentAt";

    /**
     * 排序方向（asc, desc）
     */
    private String sortDirection = "desc";

    /**
     * 是否高亮搜索结果
     */
    private Boolean highlight = true;

    /**
     * 是否包含已删除的消息
     */
    private Boolean includeDeleted = false;

    /**
     * 是否只搜索自己的消息
     */
    private Boolean onlyMyMessages = false;

    /**
     * 搜索上下文大小（返回匹配消息前后多少条消息）
     */
    @Min(value = 0, message = "上下文大小不能小于0")
    @Max(value = 10, message = "上下文大小不能超过10")
    private Integer contextSize = 2;

    /**
     * 检查是否需要高亮
     * 
     * @return 是否需要高亮
     */
    public boolean shouldHighlight() {
        return highlight != null && highlight;
    }

    /**
     * 检查是否包含已删除内容
     * 
     * @return 是否包含已删除内容
     */
    public boolean shouldIncludeDeleted() {
        return includeDeleted != null && includeDeleted;
    }

    /**
     * 检查是否只搜索自己的消息
     * 
     * @return 是否只搜索自己的消息
     */
    public boolean shouldSearchOnlyMyMessages() {
        return onlyMyMessages != null && onlyMyMessages;
    }

    /**
     * 获取上下文大小
     * 
     * @return 上下文大小
     */
    public int getContextSize() {
        return contextSize != null && contextSize >= 0 ? contextSize : 2;
    }

    /**
     * 检查是否有时间范围限制
     * 
     * @return 是否有时间范围限制
     */
    public boolean hasTimeRange() {
        return startTime != null || endTime != null;
    }

    /**
     * 检查是否有发送者限制
     * 
     * @return 是否有发送者限制
     */
    public boolean hasSenderFilter() {
        return senderIds != null && !senderIds.isEmpty();
    }

    /**
     * 检查是否有消息类型限制
     * 
     * @return 是否有消息类型限制
     */
    public boolean hasMessageTypeFilter() {
        return messageTypes != null && !messageTypes.isEmpty();
    }
}