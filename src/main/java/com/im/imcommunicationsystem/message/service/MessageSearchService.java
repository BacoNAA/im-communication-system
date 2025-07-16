package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.dto.request.ConversationSearchRequest;
import com.im.imcommunicationsystem.message.dto.request.GlobalSearchRequest;
import com.im.imcommunicationsystem.message.dto.response.MessageSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 消息搜索服务接口
 * 定义消息搜索相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface MessageSearchService {

    /**
     * 会话内搜索
     * 
     * @param request 会话内搜索请求
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 搜索结果
     */
    Page<MessageSearchResponse> searchInConversation(ConversationSearchRequest request, Pageable pageable, Long userId);

    /**
     * 全局搜索
     * 
     * @param request 全局搜索请求
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 搜索结果
     */
    Page<MessageSearchResponse> globalSearch(GlobalSearchRequest request, Pageable pageable, Long userId);

    /**
     * 索引消息到搜索引擎
     * 
     * @param messageId 消息ID
     */
    void indexMessage(Long messageId);

    /**
     * 从搜索引擎删除消息索引
     * 
     * @param messageId 消息ID
     */
    void deleteMessageIndex(Long messageId);

    /**
     * 更新消息索引
     * 
     * @param messageId 消息ID
     */
    void updateMessageIndex(Long messageId);

    /**
     * 批量索引消息
     * 
     * @param messageIds 消息ID列表
     */
    void batchIndexMessages(java.util.List<Long> messageIds);

    /**
     * 重建搜索索引
     */
    void rebuildSearchIndex();
}