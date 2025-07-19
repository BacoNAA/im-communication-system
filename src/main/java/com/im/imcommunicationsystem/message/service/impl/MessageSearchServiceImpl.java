package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.message.dto.request.ConversationSearchRequest;
import com.im.imcommunicationsystem.message.dto.request.GlobalSearchRequest;
import com.im.imcommunicationsystem.message.dto.response.MessageDTO;
import com.im.imcommunicationsystem.message.dto.response.MessageSearchResponse;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.message.service.MessageSearchService;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import com.im.imcommunicationsystem.message.enums.MessageType;

/**
 * 消息搜索服务实现类
 * 实现消息搜索相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSearchServiceImpl implements MessageSearchService {

    private final MessageRepository messageRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserProfileService userProfileService;
    private final Environment environment;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<MessageSearchResponse> searchInConversation(ConversationSearchRequest request, Pageable pageable, Long userId) {
        log.info("Searching in conversation {} with keyword '{}' by user {}", 
                request.getConversationId(), request.getKeyword(), userId);
        
        try {
            // 1. 验证用户是否有权限访问该会话
            boolean isUserInConversation = conversationMemberRepository.existsByConversationIdAndUserId(
                    request.getConversationId(), userId);
            
            if (!isUserInConversation) {
                log.warn("User {} is not a member of conversation {}", userId, request.getConversationId());
                throw new IllegalArgumentException("您不是该会话的成员，无法搜索消息");
            }
            
            // 2. 构建查询条件
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Message> query = cb.createQuery(Message.class);
            Root<Message> root = query.from(Message.class);
            
            // 基本条件：会话ID匹配且消息未被撤回
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("conversationId"), request.getConversationId()));
            predicates.add(cb.notEqual(root.get("status"), MessageStatus.RECALLED));
            
            // 关键词搜索（在消息内容中）
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("content")), 
                    "%" + request.getKeyword().toLowerCase() + "%"
                ));
            }
            
            // 消息类型过滤
            if (request.hasMessageTypeFilter()) {
                predicates.add(root.get("messageType").in(request.getMessageTypes()));
            }
            
            // 发送者过滤
            if (request.hasSenderFilter()) {
                predicates.add(root.get("senderId").in(request.getSenderIds()));
            }
            
            // 时间范围过滤
            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartTime()));
            }
            
            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndTime()));
            }
            
            // 只搜索自己的消息
            if (request.shouldSearchOnlyMyMessages()) {
                predicates.add(cb.equal(root.get("senderId"), userId));
            }
            
            // 应用所有条件
            query.where(predicates.toArray(new Predicate[0]));
            
            // 设置排序
            if ("asc".equalsIgnoreCase(request.getSortDirection())) {
                query.orderBy(cb.asc(root.get(request.getSortBy())));
            } else {
                query.orderBy(cb.desc(root.get(request.getSortBy())));
            }
            
            // 3. 执行查询
            TypedQuery<Message> typedQuery = entityManager.createQuery(query);
            
            // 获取总记录数
            int totalCount = typedQuery.getResultList().size();
            
            // 设置分页
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
            
            List<Message> messages = typedQuery.getResultList();
            
            // 4. 转换搜索结果
            List<MessageSearchResponse.MessageSearchResult> searchResults = new ArrayList<>();
            
            for (Message message : messages) {
                MessageDTO messageDTO = convertToMessageDTO(message);
                
                // 创建高亮内容
                Map<String, List<String>> highlights = new HashMap<>();
                if (request.shouldHighlight() && request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                    String content = message.getContent();
                    String keyword = request.getKeyword();
                    
                    // 简单的高亮实现，实际项目中可能需要更复杂的处理
                    String highlightedContent = content.replaceAll(
                        "(?i)" + keyword, 
                        "<span class='highlight'>$0</span>"
                    );
                    
                    highlights.put("content", Collections.singletonList(highlightedContent));
                }
                
                // 创建搜索结果项
                MessageSearchResponse.MessageSearchResult result = MessageSearchResponse.MessageSearchResult.builder()
                    .message(messageDTO)
                    .score(1.0) // 简单实现，未计算实际相关度分数
                    .highlights(highlights)
                    .matchedFields(Collections.singletonList("content"))
                    .build();
                
                searchResults.add(result);
            }
            
            // 5. 构建分页响应
            MessageSearchResponse response = MessageSearchResponse.builder()
                .results(searchResults)
                .total((long) totalCount)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) totalCount / pageable.getPageSize()))
                .hasNext(pageable.getPageNumber() < (int) Math.ceil((double) totalCount / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .keyword(request.getKeyword())
                .searchTime(System.currentTimeMillis()) // 未计算实际搜索时间
                .build();
            
            // 直接返回MessageSearchResponse对象，不再包装在Page中
            return new PageImpl<>(
                Collections.singletonList(response),
                pageable,
                1 // 总是返回1，因为我们只有一个MessageSearchResponse对象
            );
            
        } catch (Exception e) {
            log.error("Error searching in conversation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 将消息实体转换为DTO
     * 
     * @param message 消息实体
     * @return 消息DTO
     */
    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
            .id(message.getId())
            .conversationId(message.getConversationId())
            .senderId(message.getSenderId())
            .content(message.getContent())
            .messageType(message.getMessageType())
            .status(message.getStatus())
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageSearchResponse> globalSearch(GlobalSearchRequest request, Pageable pageable, Long userId) {
        log.info("Global search with keyword '{}' by user {}", request.getKeyword(), userId);
        
        try {
            // 检查Elasticsearch是否启用
            if (isElasticsearchEnabled()) {
                log.info("Elasticsearch已启用，尝试使用Elasticsearch进行搜索");
                // TODO: 实现使用Elasticsearch的全局搜索
                // 这里暂时不实现，使用数据库搜索作为备选
            }
            
            // 1. 获取用户参与的会话ID列表
            List<Long> userConversationIds = conversationMemberRepository.findConversationIdsByUserId(userId);
            
            if (userConversationIds.isEmpty()) {
                log.info("User {} has no conversations, returning empty search result", userId);
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            
            log.debug("User {} participates in {} conversations", userId, userConversationIds.size());
            
            // 2. 构建并执行Elasticsearch查询
            List<MessageSearchResponse.MessageSearchResult> searchResults = new ArrayList<>();
            long totalResults = 0;
            long searchStartTime = System.currentTimeMillis();
            
            // 这里应该使用注入的ElasticsearchOperations或MessageDocumentRepository
            // 由于还没有完全集成Elasticsearch，这里暂时使用模拟实现
            // TODO: 替换为真实的Elasticsearch查询
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Message> query = cb.createQuery(Message.class);
            Root<Message> root = query.from(Message.class);
            
            // 构建条件
            List<Predicate> predicates = new ArrayList<>();
            
            // 限定用户可访问的会话范围
            predicates.add(root.get("conversationId").in(userConversationIds));
            
            // 消息状态不是已撤回
            predicates.add(cb.notEqual(root.get("status"), MessageStatus.RECALLED));
            
            // 关键词搜索 - 改进版支持多关键词
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String[] keywords = request.getKeyword().toLowerCase().split("\\s+");
                List<Predicate> keywordPredicates = new ArrayList<>();
                
                for (String keyword : keywords) {
                    if (keyword.trim().isEmpty()) continue;
                    
                    keywordPredicates.add(cb.like(
                        cb.lower(root.get("content")), 
                        "%" + keyword.trim() + "%"
                    ));
                }
                
                if (!keywordPredicates.isEmpty()) {
                    // 多个关键词用OR连接
                    predicates.add(keywordPredicates.size() == 1 
                        ? keywordPredicates.get(0) 
                        : cb.or(keywordPredicates.toArray(new Predicate[0])));
                }
            }
            
            // 会话范围
            if (request.getConversationIds() != null && !request.getConversationIds().isEmpty()) {
                predicates.add(root.get("conversationId").in(request.getConversationIds()));
            }
            
            // 发送者ID
            if (request.getSenderIds() != null && !request.getSenderIds().isEmpty()) {
                predicates.add(root.get("senderId").in(request.getSenderIds()));
            }
            
            // 消息类型
            if (request.getMessageTypes() != null && !request.getMessageTypes().isEmpty()) {
                List<MessageType> messageTypes = request.getMessageTypes().stream()
                        .map(type -> {
                            try {
                                return MessageType.valueOf(type);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                if (!messageTypes.isEmpty()) {
                    predicates.add(root.get("messageType").in(messageTypes));
                }
            }
            
            // 时间范围
            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartTime()));
            }
            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndTime()));
            }
            
            // 应用条件
            query.where(predicates.toArray(new Predicate[0]));
            
            // 排序
            if ("asc".equalsIgnoreCase(request.getSortDirection())) {
                query.orderBy(cb.asc(root.get(request.getSortBy())));
            } else {
                query.orderBy(cb.desc(root.get(request.getSortBy())));
            }
            
            // 执行查询
            TypedQuery<Message> typedQuery = entityManager.createQuery(query);
            
            // 获取总记录数
            int totalCount = typedQuery.getResultList().size();
            
            // 设置分页
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
            
            List<Message> messages = typedQuery.getResultList();
            
            // 3. 转换搜索结果
            for (Message message : messages) {
                MessageDTO messageDTO = convertToMessageDTO(message);
                
                // 创建高亮内容 - 改进版支持多关键词
                Map<String, List<String>> highlights = new HashMap<>();
                if (request.shouldHighlight() && request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                    String content = message.getContent();
                    String highlightedContent = content;
                    
                    // 分割关键词
                    String[] keywords = request.getKeyword().toLowerCase().split("\\s+");
                    for (String keyword : keywords) {
                        if (keyword.trim().isEmpty()) continue;
                        
                        // 对每个关键词进行高亮处理
                        // 使用正则表达式的忽略大小写模式
                        highlightedContent = highlightedContent.replaceAll(
                            "(?i)" + Pattern.quote(keyword.trim()), 
                            "<span class='highlight'>$0</span>"
                        );
                    }
                    
                    highlights.put("content", Collections.singletonList(highlightedContent));
                }
                
                // 创建搜索结果项
                MessageSearchResponse.MessageSearchResult result = MessageSearchResponse.MessageSearchResult.builder()
                    .message(messageDTO)
                    .score(1.0) // 简单实现，未计算实际相关度分数
                    .highlights(highlights)
                    .matchedFields(Collections.singletonList("content"))
                    .build();
                
                searchResults.add(result);
            }
            
            // 计算搜索时间
            long searchEndTime = System.currentTimeMillis();
            long searchTime = searchEndTime - searchStartTime;
            
            // 4. 构建分页响应
            MessageSearchResponse response = MessageSearchResponse.builder()
                .results(searchResults)
                .total((long) totalCount)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) totalCount / pageable.getPageSize()))
                .hasNext(pageable.getPageNumber() < (int) Math.ceil((double) totalCount / pageable.getPageSize()) - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .keyword(request.getKeyword())
                .searchTime(searchTime)
                .build();
            
            log.info("Global search found {} results for keyword '{}'", totalCount, request.getKeyword());
            if (totalCount > 0) {
                log.debug("First result: {}", searchResults.isEmpty() ? "none" : 
                    searchResults.get(0).getMessage().getContent());
            }
            
            // 返回结果
            return new PageImpl<>(
                Collections.singletonList(response),
                pageable,
                1 // 总是返回1，因为我们只有一个MessageSearchResponse对象
            );
            
        } catch (Exception e) {
            log.error("全局搜索失败: {}", e.getMessage(), e);
            
            // 返回一个空的结果集，而不是抛出异常
            MessageSearchResponse emptyResponse = MessageSearchResponse.builder()
                .results(Collections.emptyList())
                .total(0L)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .keyword(request.getKeyword())
                .searchTime(0L)
                .build();
                
            return new PageImpl<>(
                Collections.singletonList(emptyResponse),
                pageable,
                1
            );
        }
    }

    @Override
    public void indexMessage(Long messageId) {
        try {
            // 检查Elasticsearch是否启用
            if (!isElasticsearchEnabled()) {
                log.debug("Elasticsearch未启用，跳过索引消息 {}", messageId);
                return;
            }
            
            // TODO: 实现索引消息到搜索引擎逻辑
            // 1. 获取消息详情
            // 2. 构建搜索文档
            // 3. 索引到Elasticsearch
            log.debug("Indexing message {}", messageId);
        } catch (Exception e) {
            log.error("索引消息失败 {}: {}", messageId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查Elasticsearch是否启用
     * @return 是否启用
     */
    private boolean isElasticsearchEnabled() {
        try {
            // 尝试从环境中获取配置
            return Boolean.TRUE.equals(environment.getProperty("elasticsearch.enabled", Boolean.class, false));
        } catch (Exception e) {
            log.debug("获取Elasticsearch配置失败，默认为禁用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteMessageIndex(Long messageId) {
        try {
            // 检查Elasticsearch是否启用
            if (!isElasticsearchEnabled()) {
                log.debug("Elasticsearch未启用，跳过删除消息索引 {}", messageId);
                return;
            }
            
            // TODO: 实现从搜索引擎删除消息索引逻辑
            // 1. 从Elasticsearch删除文档
            log.debug("Deleting message index {}", messageId);
        } catch (Exception e) {
            log.error("删除消息索引失败 {}: {}", messageId, e.getMessage(), e);
        }
    }

    @Override
    public void updateMessageIndex(Long messageId) {
        try {
            // 检查Elasticsearch是否启用
            if (!isElasticsearchEnabled()) {
                log.debug("Elasticsearch未启用，跳过更新消息索引 {}", messageId);
                return;
            }
            
            // TODO: 实现更新消息索引逻辑
            // 1. 获取最新消息详情
            // 2. 更新Elasticsearch文档
            log.debug("Updating message index {}", messageId);
        } catch (Exception e) {
            log.error("更新消息索引失败 {}: {}", messageId, e.getMessage(), e);
        }
    }

    @Override
    public void batchIndexMessages(List<Long> messageIds) {
        try {
            // 检查Elasticsearch是否启用
            if (!isElasticsearchEnabled()) {
                log.debug("Elasticsearch未启用，跳过批量索引消息 {}", messageIds.size());
                return;
            }
            
            // TODO: 实现批量索引消息逻辑
            // 1. 批量获取消息详情
            // 2. 构建批量索引请求
            // 3. 执行批量索引
            log.info("Batch indexing {} messages", messageIds.size());
        } catch (Exception e) {
            log.error("批量索引消息失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void rebuildSearchIndex() {
        try {
            // 检查Elasticsearch是否启用
            if (!isElasticsearchEnabled()) {
                log.debug("Elasticsearch未启用，跳过重建搜索索引");
                return;
            }
            // TODO: 实现重建搜索索引逻辑
            // 1. 清空现有索引
            // 2. 重新索引所有消息
            log.info("Rebuilding search index");
        } catch (Exception e) {
            log.error("重建搜索索引失败: {}", e.getMessage(), e);
        }
    }
}