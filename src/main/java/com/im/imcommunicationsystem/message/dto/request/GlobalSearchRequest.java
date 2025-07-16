package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 全局搜索请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchRequest {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 200, message = "搜索关键词不能超过200个字符")
    private String keyword;

    /**
     * 搜索类型（messages, contacts, conversations, files等）
     */
    private List<String> searchTypes;

    /**
     * 会话ID列表（限制搜索范围）
     */
    private List<Long> conversationIds;

    /**
     * 发送者ID列表
     */
    private List<Long> senderIds;

    /**
     * 消息类型（text, image, video, audio, file等）
     */
    private List<String> messageTypes;

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
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;

    /**
     * 排序字段
     */
    private String sortBy = "createdAt";

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
     * 检查是否搜索消息
     * 
     * @return 是否搜索消息
     */
    public boolean shouldSearchMessages() {
        return searchTypes == null || searchTypes.isEmpty() || searchTypes.contains("messages");
    }

    /**
     * 检查是否搜索联系人
     * 
     * @return 是否搜索联系人
     */
    public boolean shouldSearchContacts() {
        return searchTypes == null || searchTypes.isEmpty() || searchTypes.contains("contacts");
    }

    /**
     * 检查是否搜索会话
     * 
     * @return 是否搜索会话
     */
    public boolean shouldSearchConversations() {
        return searchTypes == null || searchTypes.isEmpty() || searchTypes.contains("conversations");
    }

    /**
     * 检查是否搜索文件
     * 
     * @return 是否搜索文件
     */
    public boolean shouldSearchFiles() {
        return searchTypes == null || searchTypes.isEmpty() || searchTypes.contains("files");
    }

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
}