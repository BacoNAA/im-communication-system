package com.im.imcommunicationsystem.common.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.enums.ConversationType;
import com.im.imcommunicationsystem.message.event.ConversationUpdateEvent;
import com.im.imcommunicationsystem.message.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Optional;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;

/**
 * WebSocket服务实现类
 * 实现WebSocket消息发送功能
 */
@Slf4j
@Service
public class WebSocketServiceImpl extends TextWebSocketHandler implements WebSocketService {

    // 存储会话映射关系：用户ID -> WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // 存储会话映射关系：WebSocketSession ID -> 用户ID
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    
    @Autowired
    public WebSocketServiceImpl(ObjectMapper objectMapper, 
                               ConversationService conversationService,
                               ApplicationEventPublisher eventPublisher,
                               GroupMemberRepository groupMemberRepository,
                               ConversationMemberRepository conversationMemberRepository) {
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.eventPublisher = eventPublisher;
        this.groupMemberRepository = groupMemberRepository;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    /**
     * 当WebSocket连接建立时调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从session中获取用户ID
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            log.info("WebSocket连接已建立，用户ID: {}, 会话ID: {}", userId, session.getId());
            userSessions.put(userId, session);
            
            // 发送连接成功消息
            try {
                Map<String, Object> connectMessage = Map.of(
                    "type", "CONNECT_SUCCESS",
                    "data", Map.of(
                        "userId", userId,
                        "timestamp", System.currentTimeMillis(),
                        "message", "WebSocket连接成功"
                    )
                );
                
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectMessage)));
                log.info("已发送连接成功消息给用户: {}", userId);
            } catch (IOException e) {
                log.error("发送连接成功消息失败", e);
            }
        } else {
            log.warn("无法识别的WebSocket连接，会话ID: {}", session.getId());
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证的连接"));
            } catch (IOException e) {
                log.error("关闭未认证的WebSocket连接失败", e);
            }
        }
    }

    /**
     * 当WebSocket连接关闭时调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            log.info("WebSocket连接已关闭，用户ID: {}, 会话ID: {}, 状态: {}", userId, session.getId(), status);
            userSessions.remove(userId);
        } else {
            log.info("未认证的WebSocket连接已关闭，会话ID: {}, 状态: {}", session.getId(), status);
        }
    }
    
    /**
     * 处理接收到的WebSocket消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("收到来自未认证会话的消息: {}", message.getPayload());
            return;
        }
        
        log.info("收到用户{}的WebSocket消息: {}", userId, message.getPayload());
        
        try {
            // 解析消息
            Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) messageMap.get("type");
            
            // 支持大小写不敏感的消息类型处理
            if (type != null) {
                String upperType = type.toUpperCase();
            
            // 根据消息类型处理
                switch (upperType) {
                case "TEST":
                    handleTestMessage(session, userId, messageMap);
                    break;
                case "PING":
                    handlePingMessage(session, userId);
                    break;
                    case "MESSAGE":
                        handleChatMessage(session, userId, messageMap);
                        break;
                    case "TYPING":
                        handleTypingMessage(session, userId, messageMap);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    break;
                }
            } else {
                log.warn("消息中没有type字段");
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理测试消息
     */
    private void handleTestMessage(WebSocketSession session, Long userId, Map<String, Object> messageMap) {
        try {
            // 构建响应消息
            Map<String, Object> response = Map.of(
                "type", "TEST_RESPONSE",
                "data", Map.of(
                    "originalMessage", messageMap,
                    "timestamp", System.currentTimeMillis(),
                    "message", "测试消息已收到"
                )
            );
            
            // 发送响应
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            log.info("已发送测试响应给用户: {}", userId);
        } catch (IOException e) {
            log.error("发送测试响应失败", e);
        }
    }
    
    /**
     * 处理ping消息
     */
    private void handlePingMessage(WebSocketSession session, Long userId) {
        try {
            // 创建pong响应消息
            Map<String, Object> pongData = new HashMap<>();
            pongData.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "PONG");
            response.put("data", pongData);
            
            // 发送pong响应
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            log.debug("已发送PONG响应给用户: {}", userId);
        } catch (IOException e) {
            log.error("发送PONG响应失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, Long userId, Map<String, Object> messageMap) {
        try {
            // 提取消息数据
            Map<String, Object> data = (Map<String, Object>) messageMap.get("data");
            if (data == null) {
                log.warn("消息数据为空");
                return;
            }
            
            // 记录完整的接收到的消息数据，用于调试
            log.info("接收到完整的消息数据: {}", objectMapper.writeValueAsString(messageMap));
            
            // 获取会话ID
            Long conversationId = null;
            if (data.containsKey("conversationId")) {
                if (data.get("conversationId") instanceof Integer) {
                    conversationId = ((Integer) data.get("conversationId")).longValue();
                } else if (data.get("conversationId") instanceof Long) {
                    conversationId = (Long) data.get("conversationId");
                } else if (data.get("conversationId") instanceof String) {
                    try {
                        conversationId = Long.parseLong((String) data.get("conversationId"));
                    } catch (NumberFormatException e) {
                        log.error("无效的会话ID格式: {}", data.get("conversationId"));
                    }
                }
            }
            
            if (conversationId == null) {
                log.warn("消息中缺少有效的会话ID");
                return;
            }
            
            // 获取消息内容
            String content = (String) data.get("content");
            if (content == null || content.trim().isEmpty()) {
                log.warn("消息内容为空");
                return;
            }
            
            // 获取消息类型
            String messageType = "TEXT"; // 默认为文本类型
            if (data.containsKey("messageType")) {
                messageType = (String) data.get("messageType");
            }
            
            // 获取媒体文件ID（如果有）
            Long mediaFileId = null;
            if (data.containsKey("mediaFileId")) {
                if (data.get("mediaFileId") instanceof Integer) {
                    mediaFileId = ((Integer) data.get("mediaFileId")).longValue();
                } else if (data.get("mediaFileId") instanceof Long) {
                    mediaFileId = (Long) data.get("mediaFileId");
                } else if (data.get("mediaFileId") instanceof String) {
                    try {
                        mediaFileId = Long.parseLong((String) data.get("mediaFileId"));
                    } catch (NumberFormatException e) {
                        log.error("无效的媒体文件ID格式: {}", data.get("mediaFileId"));
                    }
                }
            }
            
            // 获取临时消息ID（如果有）
            String tempId = null;
            if (data.containsKey("tempId")) {
                tempId = String.valueOf(data.get("tempId"));
            }
            
            // 构建消息事件对象
            WebSocketMessageEvent event = new WebSocketMessageEvent(this, conversationId, userId, content, messageType, mediaFileId, tempId);
            
            // 发布消息事件，让MessageService处理
            eventPublisher.publishEvent(event);
            
            // 生成确认消息
            Map<String, Object> confirmationData = new HashMap<>();
            confirmationData.put("tempId", tempId);
            confirmationData.put("status", "RECEIVED");
            confirmationData.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> confirmationResponse = new HashMap<>();
            confirmationResponse.put("type", "MESSAGE_CONFIRMATION");
            confirmationResponse.put("data", confirmationData);
            
            // 发送确认消息给发送者
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmationResponse)));
            
        } catch (Exception e) {
            log.error("处理聊天消息时发生错误: {}", e.getMessage(), e);
            
            try {
                // 发送错误响应给发送者
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "处理消息时发生错误");
                errorData.put("message", e.getMessage());
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "ERROR");
                errorResponse.put("data", errorData);
                
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
            } catch (IOException ex) {
                log.error("发送错误响应失败: {}", ex.getMessage());
            }
        }
    }
    
    /**
     * 处理输入状态消息
     */
    private void handleTypingMessage(WebSocketSession session, Long userId, Map<String, Object> messageMap) {
        try {
            // 提取消息数据
            Map<String, Object> data = (Map<String, Object>) messageMap.get("data");
            if (data == null) {
                log.warn("输入状态数据为空");
                return;
            }
            
            // 获取会话ID
            Long conversationId = null;
            if (data.containsKey("conversationId")) {
                if (data.get("conversationId") instanceof Integer) {
                    conversationId = ((Integer) data.get("conversationId")).longValue();
                } else if (data.get("conversationId") instanceof Long) {
                    conversationId = (Long) data.get("conversationId");
                } else if (data.get("conversationId") instanceof String) {
                    conversationId = Long.parseLong((String) data.get("conversationId"));
                }
            }
            
            if (conversationId == null) {
                log.warn("输入状态消息中没有会话ID");
                return;
            }
            
            // 获取输入状态
            boolean isTyping = Boolean.TRUE.equals(data.get("isTyping"));
            
            log.info("用户{}在会话{}中的输入状态: {}", userId, conversationId, isTyping ? "正在输入" : "停止输入");
            
            try {
                // 获取用户资料，尝试获取昵称
                String userName = "用户" + userId;
                // 这里可以调用用户服务获取用户昵称，此处简化处理
                
                // 构建输入状态消息
                Map<String, Object> typingData = new HashMap<>();
                typingData.put("userId", userId);
                typingData.put("userName", userName);
                typingData.put("conversationId", conversationId);
                typingData.put("isTyping", isTyping);
                typingData.put("timestamp", System.currentTimeMillis());
                
                Map<String, Object> typingMessage = new HashMap<>();
                typingMessage.put("type", "TYPING");
                typingMessage.put("data", typingData);
                
                // 将输入状态广播给会话中的其他成员
                sendMessageToConversation(conversationId, typingMessage, userId);
                
                // 发送确认到发送者
                Map<String, Object> confirmationData = new HashMap<>();
                confirmationData.put("conversationId", conversationId);
                confirmationData.put("status", "RECEIVED");
                confirmationData.put("timestamp", System.currentTimeMillis());
                
                Map<String, Object> confirmationMessage = new HashMap<>();
                confirmationMessage.put("type", "TYPING_CONFIRMATION");
                confirmationMessage.put("data", confirmationData);
                
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmationMessage)));
            } catch (Exception e) {
                log.error("处理输入状态附加信息失败: {}", e.getMessage());
                // 继续处理主流程
            }
            
        } catch (Exception e) {
            log.error("处理输入状态消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听会话更新事件
     * 
     * @param event 会话更新事件
     */
    @EventListener
    public void handleConversationUpdateEvent(ConversationUpdateEvent event) {
        log.info("收到会话更新事件: conversationId={}, updateType={}", 
                event.getConversationId(), event.getUpdateType());
        
        try {
            Long conversationId = event.getConversationId();
            Object conversationData = event.getConversationData();
            String updateType = event.getUpdateType();
            Long excludeUserId = event.getExcludeUserId();
            
            // 根据更新类型处理
            switch (updateType) {
                case "UPDATE":
                    // 发送会话更新通知
                    sendConversationUpdate(conversationId, conversationData, updateType, excludeUserId);
                    break;
                case "PIN":
                case "CONVERSATION_PIN":
                    // 处理会话置顶状态更新
                    handleConversationPinEvent(conversationData);
                    break;
                case "ARCHIVE":
                case "CONVERSATION_ARCHIVE":
                    // 处理会话归档状态更新
                    handleConversationArchiveEvent(conversationData);
                    break;
                case "DND":
                case "CONVERSATION_DND":
                    // 处理会话免打扰状态更新
                    handleConversationDndEvent(conversationData);
                    break;
                case "NEW":
                    // 发送新会话通知
                    sendConversationUpdate(conversationId, conversationData, updateType, excludeUserId);
                    break;
                case "DELETE":
                    // 发送会话删除通知
                    sendConversationUpdate(conversationId, conversationData, updateType, excludeUserId);
                    break;
                default:
                    log.warn("未处理的会话更新类型: {}", updateType);
                    break;
            }
        } catch (Exception e) {
            log.error("处理会话更新事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理会话置顶状态更新事件
     * 
     * @param eventData 事件数据
     */
    private void handleConversationPinEvent(Object eventData) {
        try {
            if (!(eventData instanceof Map)) {
                log.warn("会话置顶事件数据格式不正确");
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;
            
            // 获取必要数据
            Long conversationId = getLongValue(data, "conversationId");
            Boolean isPinned = (Boolean) data.get("isPinned");
            Long userId = getLongValue(data, "userId");
            
            if (conversationId == null || isPinned == null || userId == null) {
                log.warn("会话置顶事件缺少必要数据");
                return;
            }
            
            // 创建WebSocket消息
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("conversationId", conversationId);
            updateData.put("isPinned", isPinned);
            updateData.put("userId", userId);
            
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "CONVERSATION_PIN");
            wsMessage.put("data", updateData);
            
            // 获取会话的所有成员
            List<Long> memberIds = conversationService.getConversationMemberIds(conversationId);
            
            if (memberIds.isEmpty()) {
                log.warn("会话 {} 没有成员，无法发送置顶状态通知", conversationId);
                return;
            }
            
            // 向会话的所有成员发送通知
            for (Long memberId : memberIds) {
                sendMessageToUser(memberId, wsMessage);
                log.debug("已发送会话置顶状态通知给用户 {}: conversationId={}, isPinned={}", 
                        memberId, conversationId, isPinned);
            }
            
            log.info("已向会话 {} 的 {} 名成员发送置顶状态通知: isPinned={}", 
                    conversationId, memberIds.size(), isPinned);
        } catch (Exception e) {
            log.error("处理会话置顶事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理会话归档状态更新事件
     * 
     * @param eventData 事件数据
     */
    private void handleConversationArchiveEvent(Object eventData) {
        try {
            if (!(eventData instanceof Map)) {
                log.warn("会话归档事件数据格式不正确");
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;
            
            // 获取必要数据
            Long conversationId = getLongValue(data, "conversationId");
            Boolean isArchived = (Boolean) data.get("isArchived");
            Long userId = getLongValue(data, "userId");
            
            if (conversationId == null || isArchived == null || userId == null) {
                log.warn("会话归档事件缺少必要数据");
                return;
            }
            
            // 创建WebSocket消息
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("conversationId", conversationId);
            updateData.put("isArchived", isArchived);
            updateData.put("userId", userId);
            
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "CONVERSATION_ARCHIVE");
            wsMessage.put("data", updateData);
            
            // 获取会话的所有成员
            List<Long> memberIds = conversationService.getConversationMemberIds(conversationId);
            
            if (memberIds.isEmpty()) {
                log.warn("会话 {} 没有成员，无法发送归档状态通知", conversationId);
                return;
            }
            
            // 向会话的所有成员发送通知
            for (Long memberId : memberIds) {
                sendMessageToUser(memberId, wsMessage);
                log.debug("已发送会话归档状态通知给用户 {}: conversationId={}, isArchived={}", 
                        memberId, conversationId, isArchived);
            }
            
            log.info("已向会话 {} 的 {} 名成员发送归档状态通知: isArchived={}", 
                    conversationId, memberIds.size(), isArchived);
        } catch (Exception e) {
            log.error("处理会话归档事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理会话免打扰状态更新事件
     * 
     * @param eventData 事件数据
     */
    private void handleConversationDndEvent(Object eventData) {
        try {
            if (!(eventData instanceof Map)) {
                log.warn("会话免打扰事件数据格式不正确");
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;
            
            // 获取必要数据
            Long conversationId = getLongValue(data, "conversationId");
            Boolean isDnd = (Boolean) data.get("isDnd");
            Long userId = getLongValue(data, "userId");
            
            if (conversationId == null || isDnd == null || userId == null) {
                log.warn("会话免打扰事件缺少必要数据");
                return;
            }
            
            // 创建WebSocket消息
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("conversationId", conversationId);
            updateData.put("isDnd", isDnd);
            updateData.put("userId", userId);
            
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "CONVERSATION_DND");
            wsMessage.put("data", updateData);
            
            // 获取会话的所有成员
            List<Long> memberIds = conversationService.getConversationMemberIds(conversationId);
            
            if (memberIds.isEmpty()) {
                log.warn("会话 {} 没有成员，无法发送免打扰状态通知", conversationId);
                return;
            }
            
            // 向会话的所有成员发送通知
            for (Long memberId : memberIds) {
                sendMessageToUser(memberId, wsMessage);
                log.debug("已发送会话免打扰状态通知给用户 {}: conversationId={}, isDnd={}", 
                        memberId, conversationId, isDnd);
            }
            
            log.info("已向会话 {} 的 {} 名成员发送免打扰状态通知: isDnd={}", 
                    conversationId, memberIds.size(), isDnd);
        } catch (Exception e) {
            log.error("处理会话免打扰事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从Map中安全获取Long值
     * 
     * @param map Map对象
     * @param key 键
     * @return Long值，如果不存在或类型不匹配则返回null
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }

    /**
     * 从WebSocket会话中获取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    /**
     * 发送消息给指定用户
     */
    @Override
    public void sendMessageToUser(Long userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageJson));
                log.debug("消息已发送给用户: {}", userId);
            } catch (IOException e) {
                log.error("发送消息给用户{}失败", userId, e);
            }
        } else {
            log.debug("用户{}不在线或WebSocket会话已关闭", userId);
        }
    }

    /**
     * 发送消息给指定会话的所有成员
     */
    @Override
    public void sendMessageToConversation(Long conversationId, Object message, Long excludeUserId) {
        try {
            // 获取会话信息
            Conversation conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                log.error("无法发送消息: 会话 {} 不存在", conversationId);
                return;
            }
            
            // 获取会话成员
            List<Long> memberIds = conversationService.getConversationMemberIds(conversationId);
            
            log.info("获取到会话{}的成员列表: {}，总成员数: {}", conversationId, memberIds, memberIds.size());
            
            // 如果是群聊会话，需要额外验证群成员关系
            if (conversation.getConversationType() == ConversationType.GROUP && conversation.getRelatedGroupId() != null) {
                Long groupId = conversation.getRelatedGroupId();
                log.info("检测到群聊会话，群组ID: {}", groupId);
                
                // 获取群组成员ID列表
                List<Long> groupMemberIds;
                try {
                    // 从GroupMemberRepository获取当前群成员列表
                    groupMemberIds = groupMemberRepository.findByIdGroupId(groupId)
                        .stream()
                        .map(member -> member.getId().getUserId())
                        .collect(Collectors.toList());
                    
                    log.info("获取到群组{}的成员列表: {}，总成员数: {}", groupId, groupMemberIds, groupMemberIds.size());
                    
                    // 过滤会话成员列表，只保留当前仍在群组中的成员
                    memberIds = memberIds.stream()
                        .filter(memberId -> groupMemberIds.contains(memberId))
                        .collect(Collectors.toList());
                    
                    log.info("过滤后的有效会话成员: {}，有效成员数: {}", memberIds, memberIds.size());
                } catch (Exception e) {
                    log.error("获取群组成员失败: {}", e.getMessage(), e);
                    // 出错时继续使用原始会话成员列表
                }
            }
            
            // 提取消息ID（如果存在）
            Long messageId = null;
            if (message instanceof Map) {
                Map<String, Object> msgMap = (Map<String, Object>) message;
                if (msgMap.containsKey("data") && msgMap.get("data") instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) msgMap.get("data");
                    if (dataMap.containsKey("id")) {
                        Object idObj = dataMap.get("id");
                        if (idObj instanceof Number) {
                            messageId = ((Number) idObj).longValue();
                        } else if (idObj instanceof String) {
                            try {
                                messageId = Long.parseLong((String) idObj);
                            } catch (NumberFormatException e) {
                                // 非数字ID，忽略
                            }
                        }
                    }
                }
            }
            
            log.debug("消息ID: {}, 类型: {}", messageId, (messageId != null ? messageId.getClass().getName() : "null"));
            
            // 转换消息为JSON字符串
            String messageJson = objectMapper.writeValueAsString(message);
            
            // 在线成员计数
            int onlineMemberCount = 0;
            
            // 发送消息给每个在线成员
            for (Long memberId : memberIds) {
                // 排除指定用户（通常是消息发送者自己）
                if (excludeUserId != null && memberId.equals(excludeUserId)) {
                    log.debug("跳过发送者自己: {}", excludeUserId);
                    continue;
                }
                
                // 检查拉黑状态 - 获取last_acceptable_message_id
                // 拉黑机制说明：
                // 1. 当用户A拉黑用户B时，设置A的last_acceptable_message_id为拉黑时的最后一条消息ID
                // 2. 用户B仍可以发送消息，但这些消息不会通过WebSocket发送给A
                // 3. 如果消息ID大于A的last_acceptable_message_id，则不发送给A
                // 4. 当A解除对B的拉黑时，将A的last_acceptable_message_id设为null，恢复正常的消息接收
                boolean shouldSkip = false;
                
                if (messageId != null && conversation.getConversationType() == ConversationType.PRIVATE) {
                    try {
                        // 获取会话成员信息
                        Optional<ConversationMember> memberOpt = conversationMemberRepository.findById(
                                new ConversationMemberId(conversationId, memberId));
                        
                        if (memberOpt.isPresent()) {
                            ConversationMember member = memberOpt.get();
                            Long lastAcceptableMessageId = member.getLastAcceptableMessageId();
                            
                            // 如果设置了last_acceptable_message_id，且当前消息ID大于该值，则跳过发送
                            if (lastAcceptableMessageId != null && messageId > lastAcceptableMessageId) {
                                log.info("消息ID {} 大于用户 {} 设置的last_acceptable_message_id {}, 跳过发送",
                                        messageId, memberId, lastAcceptableMessageId);
                                shouldSkip = true;
                            }
                        }
                    } catch (Exception e) {
                        log.error("检查拉黑状态失败: conversationId={}, memberId={}, error={}",
                                conversationId, memberId, e.getMessage(), e);
                        // 出错时继续发送
                    }
                }
                
                if (shouldSkip) {
                    log.info("用户 {} 已拉黑，跳过发送消息 {} 给会话 {}", memberId, messageId, conversationId);
                    continue;
                }
                
                WebSocketSession session = userSessions.get(memberId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        log.info("消息已发送给会话{}的成员: {}", conversationId, memberId);
                        onlineMemberCount++;
                    } catch (IOException e) {
                        log.error("发送消息给会话{}的成员{}失败: {}", conversationId, memberId, e.getMessage(), e);
                    }
                } else {
                    log.info("会话{}的成员{}不在线或WebSocket会话已关闭", conversationId, memberId);
                    if (session == null) {
                        log.debug("成员{}的会话为null", memberId);
                    } else {
                        log.debug("成员{}的会话已关闭，状态: {}", memberId, session.isOpen() ? "开启" : "关闭");
                    }
                }
            }
            
            log.info("会话{}消息发送完成，在线成员数: {}/{}", conversationId, onlineMemberCount, memberIds.size() - (excludeUserId != null ? 1 : 0));
        } catch (Exception e) {
            log.error("发送消息给会话{}的成员失败: {}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * 广播消息给所有连接的用户
     */
    @Override
    public void broadcastMessage(Object message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("广播消息给用户{}失败", entry.getKey(), e);
                    }
                }
            }
            log.debug("消息已广播给所有在线用户");
        } catch (IOException e) {
            log.error("广播消息失败", e);
        }
    }

    @Override
    public void sendConversationUpdate(Long conversationId, Object conversationData, String updateType, Long excludeUserId) {
        try {
            // 获取会话成员
            List<Long> memberIds = conversationService.getConversationMemberIds(conversationId);
            
            log.info("发送会话更新通知给会话{}的成员，更新类型: {}, 总成员数: {}", conversationId, updateType, memberIds.size());
            
            // 创建会话更新消息
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("conversationId", conversationId);
            updateData.put("data", conversationData);
            updateData.put("updateType", updateType);
            updateData.put("timestamp", System.currentTimeMillis());
            
            // 创建WebSocket消息
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "CONVERSATION_UPDATE");
            wsMessage.put("data", updateData);
            
            // 转换消息为JSON字符串
            String messageJson = objectMapper.writeValueAsString(wsMessage);
            
            // 在线成员计数
            int onlineMemberCount = 0;
            
            // 发送消息给每个在线成员
            for (Long memberId : memberIds) {
                // 排除指定用户（如果有）
                if (excludeUserId != null && memberId.equals(excludeUserId)) {
                    log.debug("跳过用户: {}", excludeUserId);
                    continue;
                }
                
                WebSocketSession session = userSessions.get(memberId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        onlineMemberCount++;
                        log.debug("会话更新通知已发送给用户: {}", memberId);
                    } catch (IOException e) {
                        log.error("向用户{}发送会话更新通知失败: {}", memberId, e.getMessage(), e);
                    }
                } else {
                    log.debug("用户{}不在线或会话已关闭", memberId);
                }
            }
            
            log.info("会话更新通知已发送给{}个在线成员", onlineMemberCount);
        } catch (Exception e) {
            log.error("发送会话更新通知失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送群组更新通知给指定群组的所有成员
     */
    @Override
    public void sendGroupUpdate(Long groupId, Object groupData, String updateType, Long excludeUserId) {
        try {
            // 获取群组成员ID列表
            List<Long> memberIds = groupMemberRepository.findByIdGroupId(groupId)
                    .stream()
                    .map(member -> member.getId().getUserId())
                    .collect(Collectors.toList());
            
            log.info("发送群组更新通知给群组{}的成员，更新类型: {}, 总成员数: {}", groupId, updateType, memberIds.size());
            
            // 如果是成员变更类型的更新，需要特殊处理
            if ("MEMBER_LEAVE".equals(updateType) || "MEMBER_REMOVED".equals(updateType)) {
                // 对于成员离开/被移除的通知，确保被移除的成员也能收到通知
                // 从groupData中提取被移除的成员ID
                try {
                    if (groupData instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) groupData;
                        if (dataMap.containsKey("userId")) {
                            Long removedUserId = getLongValue(dataMap, "userId");
                            if (removedUserId != null && !memberIds.contains(removedUserId)) {
                                // 添加被移除的成员到通知列表
                                log.info("添加被移除的成员{}到通知列表", removedUserId);
                                memberIds.add(removedUserId);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("处理被移除成员通知时出错: {}", e.getMessage(), e);
                }
            }
            
            // 创建群组更新消息
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("groupId", groupId);
            updateData.put("data", groupData);
            updateData.put("updateType", updateType);
            updateData.put("timestamp", System.currentTimeMillis());
            
            // 创建WebSocket消息
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "GROUP_UPDATE");
            wsMessage.put("data", updateData);
            
            // 转换消息为JSON字符串
            String messageJson = objectMapper.writeValueAsString(wsMessage);
            
            // 在线成员计数
            int onlineMemberCount = 0;
            
            // 发送消息给每个在线成员
            for (Long memberId : memberIds) {
                // 排除指定用户（如果有）
                if (excludeUserId != null && memberId.equals(excludeUserId)) {
                    log.debug("跳过用户: {}", excludeUserId);
                    continue;
                }
                
                WebSocketSession session = userSessions.get(memberId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        onlineMemberCount++;
                        log.debug("群组更新通知已发送给用户: {}", memberId);
                    } catch (IOException e) {
                        log.error("向用户{}发送群组更新通知失败: {}", memberId, e.getMessage(), e);
                    }
                } else {
                    log.debug("用户{}不在线或会话已关闭", memberId);
                }
            }
            
            log.info("群组更新通知已发送给{}个在线成员", onlineMemberCount);
        } catch (Exception e) {
            log.error("发送群组更新通知失败: {}", e.getMessage(), e);
        }
    }

    // 添加一个消息事件类
    public static class WebSocketMessageEvent {
        private final Object source;
        private final Long conversationId;
        private final Long senderId;
        private final String content;
        private final String messageType;
        private final Long mediaFileId;
        private final String tempId;

        public WebSocketMessageEvent(Object source, Long conversationId, Long senderId, 
                                    String content, String messageType, Long mediaFileId, 
                                    String tempId) {
            this.source = source;
            this.conversationId = conversationId;
            this.senderId = senderId;
            this.content = content;
            this.messageType = messageType;
            this.mediaFileId = mediaFileId;
            this.tempId = tempId;
        }

        public Object getSource() {
            return source;
        }

        public Long getConversationId() {
            return conversationId;
        }

        public Long getSenderId() {
            return senderId;
        }

        public String getContent() {
            return content;
        }

        public String getMessageType() {
            return messageType;
        }

        public Long getMediaFileId() {
            return mediaFileId;
        }

        public String getTempId() {
            return tempId;
        }
    }
} 