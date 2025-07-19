package com.im.imcommunicationsystem.message.repository.elasticsearch;

import com.im.imcommunicationsystem.message.model.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息文档仓库接口
 * 用于与Elasticsearch进行交互
 */
@Repository
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public interface MessageDocumentRepository extends ElasticsearchRepository<MessageDocument, Long> {

    /**
     * 根据会话ID查询消息
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<MessageDocument> findByConversationId(Long conversationId);
    
    /**
     * 根据关键词全文检索消息
     * @param keyword 关键词
     * @return 消息列表
     */
    List<MessageDocument> findByContentContaining(String keyword);
    
    /**
     * 根据会话ID和关键词全文检索消息
     * @param conversationId 会话ID
     * @param keyword 关键词
     * @return 消息列表
     */
    List<MessageDocument> findByConversationIdAndContentContaining(Long conversationId, String keyword);
    
    /**
     * 根据发送者ID查询消息
     * @param senderId 发送者ID
     * @return 消息列表
     */
    List<MessageDocument> findBySenderId(Long senderId);
    
    /**
     * 根据创建时间范围查询消息
     * @param start 开始时间
     * @param end 结束时间
     * @return 消息列表
     */
    List<MessageDocument> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
} 