package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.message.event.ConversationUpdateEvent;
import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.ConversationDTO;
import com.im.imcommunicationsystem.message.dto.response.ConversationMemberDTO;
import com.im.imcommunicationsystem.message.dto.response.ConversationResponse;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;
import com.im.imcommunicationsystem.message.enums.ConversationType;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.repository.ConversationRepository;
import com.im.imcommunicationsystem.message.service.ConversationMemberService;
import com.im.imcommunicationsystem.message.service.ConversationService;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.im.imcommunicationsystem.message.service.ReadStatusService;

/**
 * 会话服务实现类
 * 实现会话管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserProfileService userProfileService;
    private final ContactService contactService;
    private final ApplicationEventPublisher eventPublisher;
    // 添加ReadStatusService依赖
    private final ReadStatusService readStatusService;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(Pageable pageable, Long userId) {
        log.info("Getting conversations for user {}", userId);
        
        try {
            // 直接使用Repository方法获取用户参与的所有会话（包括创建的和被邀请加入的）
            List<Conversation> conversations = conversationRepository.findByParticipantUserId(userId);
            log.info("Found {} conversations for user {}", conversations.size(), userId);
        
            if (conversations.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
            // 过滤未删除的会话
        conversations = conversations.stream()
                .filter(conv -> !conv.isDeleted())
                .collect(Collectors.toList());
            log.info("After filtering deleted conversations: {} remain", conversations.size());
        
            // 构建会话DTO列表
        List<ConversationDTO> conversationDTOs = conversations.stream()
                .map(conversation -> buildConversationDTO(conversation, userId))
                .filter(Objects::nonNull)
                .filter(dto -> {
                    // 过滤掉已归档的会话
                    if (dto.getCurrentUserParticipant() != null) {
                        return !Boolean.TRUE.equals(dto.getCurrentUserParticipant().getIsArchived());
                    }
                    return true;
                })
                .sorted((a, b) -> {
                    // 置顶的会话排在前面
                    if (a.getIsPinned() != b.getIsPinned()) {
                        return a.getIsPinned() ? -1 : 1;
                    }
                    // 按最后活跃时间倒序排列，处理null值情况
                    LocalDateTime timeA = a.getLastActiveAt();
                    LocalDateTime timeB = b.getLastActiveAt();
                    
                    if (timeA == null && timeB == null) {
                        return 0; // 两者都为null，视为相等
                    } else if (timeA == null) {
                        return 1; // a为null，b不为null，b排前面
                    } else if (timeB == null) {
                        return -1; // a不为null，b为null，a排前面
                    } else {
                        return timeB.compareTo(timeA); // 两者都不为null，正常比较
                    }
                })
                .collect(Collectors.toList());
        
            // 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), conversationDTOs.size());
        
        List<ConversationDTO> pageContent = start < conversationDTOs.size() ? 
                conversationDTOs.subList(start, end) : Collections.emptyList();
        
        // 直接创建一个ConversationResponse对象，包含会话列表
        ConversationResponse response = ConversationResponse.success(pageContent);
        
        // 将这个响应对象放入一个单元素列表中，并返回
        return new PageImpl<>(Collections.singletonList(response), pageable, 1);
        } catch (Exception e) {
            log.error("Error getting conversations for user {}: {}", userId, e.getMessage(), e);
            // 返回空结果而不是抛出异常，避免前端崩溃
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getArchivedConversations(Pageable pageable, Long userId) {
        log.info("Getting archived conversations for user {}", userId);
        
        try {
            // 获取用户参与的所有会话
            List<Conversation> conversations = conversationRepository.findByParticipantUserId(userId);
            log.info("Found {} conversations for user {}", conversations.size(), userId);
        
            if (conversations.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        
            // 过滤未删除的会话
            conversations = conversations.stream()
                    .filter(conv -> !conv.isDeleted())
                    .collect(Collectors.toList());
            log.info("After filtering deleted conversations: {} remain", conversations.size());
        
            // 构建会话DTO列表
            List<ConversationDTO> conversationDTOs = conversations.stream()
                    .map(conversation -> buildConversationDTO(conversation, userId))
                    .filter(Objects::nonNull)
                    .filter(dto -> {
                        // 只保留已归档的会话
                        if (dto.getCurrentUserParticipant() != null) {
                            return Boolean.TRUE.equals(dto.getCurrentUserParticipant().getIsArchived());
                        }
                        return false;
                    })
                    .sorted((a, b) -> {
                        // 按最后活跃时间倒序排列，处理null值情况
                        LocalDateTime timeA = a.getLastActiveAt();
                        LocalDateTime timeB = b.getLastActiveAt();
                        
                        if (timeA == null && timeB == null) {
                            return 0; // 两者都为null，视为相等
                        } else if (timeA == null) {
                            return 1; // a为null，b不为null，b排前面
                        } else if (timeB == null) {
                            return -1; // a不为null，b为null，a排前面
                        } else {
                            return timeB.compareTo(timeA); // 两者都不为null，正常比较
                        }
                    })
                    .collect(Collectors.toList());
        
            // 分页处理
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), conversationDTOs.size());
        
            List<ConversationDTO> pageContent = start < conversationDTOs.size() ? 
                    conversationDTOs.subList(start, end) : Collections.emptyList();
        
            // 创建响应对象
            ConversationResponse response = ConversationResponse.success(pageContent);
        
            // 返回分页结果
            return new PageImpl<>(Collections.singletonList(response), pageable, 1);
        } catch (Exception e) {
            log.error("Error getting archived conversations for user {}: {}", userId, e.getMessage(), e);
            // 返回空结果而不是抛出异常，避免前端崩溃
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
    
    /**
     * 构建会话DTO
     * 
     * @param conversation 会话实体
     * @param userId 用户ID
     * @return 会话DTO
     */
    public ConversationDTO buildConversationDTO(Conversation conversation, Long userId) {
        try {
            log.info("开始构建会话DTO: conversationId={}, userId={}", conversation.getId(), userId);
            ConversationDTO dto = new ConversationDTO();
            
            // 基本信息
            dto.setId(conversation.getId());
            dto.setType(conversation.getConversationType());
            dto.setName(conversation.getName());
            dto.setDescription(conversation.getDescription());
            dto.setAvatarUrl(conversation.getAvatarUrl());
            dto.setCreatorId(conversation.getCreatedBy());
            dto.setLastActiveAt(conversation.getLastActiveAt());
            dto.setSettings(conversation.getSettings());
            dto.setMetadata(conversation.getMetadata());
            dto.setCreatedAt(conversation.getCreatedAt());
            dto.setUpdatedAt(conversation.getUpdatedAt());
            
            // 获取用户在此会话中的成员信息
            log.debug("获取用户在会话中的成员信息: conversationId={}, userId={}", conversation.getId(), userId);
            ConversationMember member = null;
            try {
                member = conversationMemberRepository
                    .findByConversationIdAndUserId(conversation.getId(), userId)
                    .orElse(null);
            } catch (Exception e) {
                log.error("获取会话成员信息失败: conversationId={}, userId={}, error={}", 
                          conversation.getId(), userId, e.getMessage(), e);
            }
            
            if (member != null) {
                dto.setIsPinned(member.getIsPinned());
                dto.setIsArchived(member.getIsArchived());
                dto.setIsDnd(member.getIsDnd());
                dto.setDraft(member.getDraft());
                log.debug("成功获取会话成员信息: isPinned={}, isArchived={}, isDnd={}", 
                         member.getIsPinned(), member.getIsArchived(), member.getIsDnd());
            } else {
                log.warn("未找到会话成员信息: conversationId={}, userId={}", conversation.getId(), userId);
                // 设置默认值
                dto.setIsPinned(false);
                dto.setIsArchived(false);
                dto.setIsDnd(false);
            }
            
            // 获取参与者信息
            log.debug("开始获取会话参与者列表: conversationId={}", conversation.getId());
            List<ConversationMember> members = null;
            try {
                members = conversationMemberRepository.findByConversationId(conversation.getId());
                log.debug("成功获取会话参与者列表: conversationId={}, count={}", 
                         conversation.getId(), members.size());
            } catch (Exception e) {
                log.error("获取会话参与者列表失败: conversationId={}, error={}", 
                          conversation.getId(), e.getMessage(), e);
                members = new ArrayList<>();
            }
            
            List<ConversationMemberDTO> participants = new ArrayList<>();
            ConversationMemberDTO currentUserParticipant = null;
            
            for (ConversationMember conversationMember : members) {
                try {
                    log.debug("处理会话参与者: conversationId={}, participantUserId={}", 
                             conversation.getId(), conversationMember.getUserId());
                    
                    // 获取用户资料信息
                    UserProfileResponse userProfile = null;
                    try {
                        userProfile = userProfileService.getUserProfile(conversationMember.getUserId());
                        log.debug("成功获取用户资料: userId={}, nickname={}", 
                                 conversationMember.getUserId(), userProfile.getNickname());
                    } catch (Exception e) {
                        log.error("获取用户资料失败: userId={}, error={}", 
                                  conversationMember.getUserId(), e.getMessage(), e);
                        // 创建一个基本的用户资料，避免空指针异常
                        userProfile = new UserProfileResponse();
                        userProfile.setId(conversationMember.getUserId());
                        userProfile.setNickname("用户" + conversationMember.getUserId());
                    }
                    
                    String alias = null;
if (!conversationMember.getUserId().equals(userId) && conversation.getConversationType().isPrivate()) {
                        try {
    alias = contactService.getContactDetail(userId, conversationMember.getUserId())
        .map(ContactResponse::getAlias)
        .orElse(null);
                            log.debug("成功获取联系人别名: userId={}, contactId={}, alias={}", 
                                     userId, conversationMember.getUserId(), alias);
                        } catch (Exception e) {
                            log.error("获取联系人别名失败: userId={}, contactId={}, error={}", 
                                      userId, conversationMember.getUserId(), e.getMessage(), e);
                        }
                    }
                    
ConversationMemberDTO participantDTO = ConversationMemberDTO.builder()
                            .conversationId(conversation.getId())
                            .userId(conversationMember.getUserId())
                            .user(userProfile)
                            .alias(alias)
                            .isPinned(conversationMember.getIsPinned())
                            .isArchived(conversationMember.getIsArchived())
                            .isDnd(conversationMember.getIsDnd())
                            .draft(conversationMember.getDraft())
                            .lastReadMessageId(conversationMember.getLastReadMessageId())
                            .build();
                    
                    participants.add(participantDTO);
                    
                    // 设置当前用户的参与者信息
                    if (conversationMember.getUserId().equals(userId)) {
                        currentUserParticipant = participantDTO;
                        log.debug("设置当前用户参与者信息: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.error("处理会话参与者失败: userId={}, error={}", 
                              conversationMember.getUserId(), e.getMessage(), e);
                }
            }
            
            dto.setParticipants(participants);
            dto.setCurrentUserParticipant(currentUserParticipant);
            
            // 获取最后一条消息
            // TODO: 实现获取最后一条消息逻辑
            // dto.setLastMessage(...);

            // 获取未读消息数量 - 使用ReadStatusService计算未读消息数量
            try {
                // 使用ReadStatusService获取未读消息数量，确保不计算自己发送的消息
                Long unreadCount = readStatusService.countUnreadMessages(userId, conversation.getId());
                dto.setUnreadCount(unreadCount.intValue());
                log.debug("获取会话未读消息数量: conversationId={}, userId={}, unreadCount={}", 
                        conversation.getId(), userId, unreadCount);
            } catch (Exception e) {
                log.error("获取未读消息数量失败: conversationId={}, userId={}, error={}", 
                        conversation.getId(), userId, e.getMessage(), e);
                dto.setUnreadCount(0); // 获取失败时默认为0
            }

            // 获取参与者数量
            dto.setParticipantCount(participants.size());
            
            log.info("成功构建会话DTO: conversationId={}, participantCount={}, unreadCount={}", 
                    conversation.getId(), participants.size(), dto.getUnreadCount());
            return dto;
        } catch (Exception e) {
            log.error("构建会话DTO失败: conversationId={}, userId={}, error={}", 
                     conversation.getId(), userId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request, Long userId) {
        // TODO: 实现创建会话逻辑
        // 1. 验证参与者权限
        // 2. 创建会话实体
        // 3. 添加参与者
        // 4. 发送WebSocket通知
        log.info("Creating conversation by user {}", userId);
        return null;
    }

    @Override
    public ConversationResponse updateConversationSettings(Long conversationId, UpdateConversationSettingsRequest request, Long userId) {
        // TODO: 实现更新会话设置逻辑
        // 1. 验证会话权限
        // 2. 更新会话设置
        // 3. 发送WebSocket通知
        log.info("Updating conversation {} settings by user {}", conversationId, userId);
        return null;
    }

    @Override
    public void pinConversation(Long conversationId, PinConversationRequest request, Long userId) {
        log.info("Pinning conversation {} for user {}, isPinned={}", conversationId, userId, request.getPinned());
        
        try {
            // 1. 验证会话权限
            if (!isUserInConversation(conversationId, userId)) {
                log.warn("User {} is not in conversation {}", userId, conversationId);
                throw new IllegalArgumentException("您不是该会话的成员，无法操作");
            }
            
            // 2. 获取会话成员关系
            ConversationMemberId memberId = new ConversationMemberId(conversationId, userId);
            ConversationMember member = conversationMemberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("会话成员关系不存在"));
            
            // 3. 更新置顶状态
            member.setIsPinned(request.getPinned());
            
            // 4. 保存更新
            conversationMemberRepository.save(member);
            
            log.info("Successfully pinned conversation {} for user {}", conversationId, userId);
            
            // 5. 发送WebSocket通知
            try {
                // 获取会话信息
                Conversation conversation = getConversationById(conversationId);
                
                // 构建会话DTO
                ConversationDTO conversationDTO = buildConversationDTO(conversation, userId);
                
                if (conversationDTO != null) {
                    // 设置置顶状态
                    conversationDTO.setIsPinned(request.getPinned());
                    
                    // 获取会话的所有成员ID
                    List<Long> memberIds = getConversationMemberIds(conversationId);
                    log.info("获取到会话 {} 的所有成员: {}", conversationId, memberIds);
                    
                    // 向所有会话成员广播更新
                    for (Long memberUserId : memberIds) {
                        // 为每个用户创建特定的更新数据
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("conversationId", conversationId);
                        updateData.put("isPinned", request.getPinned());
                        updateData.put("userId", memberUserId); // 设置为接收通知的用户ID
                        
                        eventPublisher.publishEvent(new ConversationUpdateEvent(
                            this,
                            conversationId,
                            updateData,
                            "CONVERSATION_PIN",
                            null // 由WebSocketService决定只发给特定用户
                        ));
                        
                        log.info("已发布会话置顶状态事件给用户 {}: conversationId={}, isPinned={}", 
                                memberUserId, conversationId, request.getPinned());
                    }
            }
            } catch (Exception e) {
                log.error("发布会话置顶状态事件失败: {}", e.getMessage(), e);
                // 不影响主流程继续执行
            }
        } catch (Exception e) {
            log.error("Failed to pin conversation {} for user {}: {}", conversationId, userId, e.getMessage(), e);
            throw new RuntimeException("置顶会话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void archiveConversation(Long conversationId, ArchiveConversationRequest request, Long userId) {
        log.info("Archiving conversation {} by user {}, isArchived={}", conversationId, userId, request.getIsArchived());
        
        try {
        // 1. 验证会话权限
            if (!isUserInConversation(conversationId, userId)) {
                log.warn("User {} is not in conversation {}, cannot archive", userId, conversationId);
                throw new IllegalArgumentException("您不是此会话的成员，无法归档");
            }
            
            // 2. 获取会话成员记录
            ConversationMemberId id = new ConversationMemberId(conversationId, userId);
            ConversationMember member = conversationMemberRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("会话成员记录不存在"));
            
            // 3. 更新归档状态
            member.setIsArchived(request.getIsArchived());
            
            // 4. 保存更新
            conversationMemberRepository.save(member);
            
            log.info("Successfully {} conversation {} for user {}", 
                    request.getIsArchived() ? "archived" : "unarchived", 
                    conversationId, userId);
                    
            // 5. 发送WebSocket通知
            try {
                // 获取会话信息
                Conversation conversation = getConversationById(conversationId);
                
                // 构建会话DTO
                ConversationDTO conversationDTO = buildConversationDTO(conversation, userId);
                
                if (conversationDTO != null) {
                    // 设置归档状态
                    conversationDTO.setIsArchived(request.getIsArchived());
                    
                    // 获取会话的所有成员ID
                    List<Long> memberIds = getConversationMemberIds(conversationId);
                    log.info("获取到会话 {} 的所有成员: {}", conversationId, memberIds);
                    
                    // 向所有会话成员广播更新
                    for (Long memberUserId : memberIds) {
                        // 为每个用户创建特定的更新数据
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("conversationId", conversationId);
                        updateData.put("isArchived", request.getIsArchived());
                        updateData.put("userId", memberUserId); // 设置为接收通知的用户ID
                        
                        eventPublisher.publishEvent(new ConversationUpdateEvent(
                            this,
                            conversationId,
                            updateData,
                            "CONVERSATION_ARCHIVE",
                            null // 由WebSocketService决定只发给特定用户
                        ));
                        
                        log.info("已发布会话归档状态事件给用户 {}: conversationId={}, isArchived={}", 
                                memberUserId, conversationId, request.getIsArchived());
                    }
                }
            } catch (Exception e) {
                log.error("发布会话归档状态事件失败: {}", e.getMessage(), e);
                // 不影响主流程继续执行
            }
        } catch (Exception e) {
            log.error("Failed to archive conversation: conversationId={}, userId={}, error={}", 
                    conversationId, userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void saveDraft(Long conversationId, SaveDraftRequest request, Long userId) {
        // TODO: 实现保存草稿逻辑
        // 1. 验证会话权限
        // 2. 保存草稿内容
        log.info("Saving draft for conversation {} by user {}", conversationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversationById(Long conversationId) {
        log.debug("Getting conversation by ID: {}", conversationId);
        
        try {
            return conversationRepository.findById(conversationId).orElse(null);
        } catch (Exception e) {
            log.error("Failed to get conversation by ID {}: {}", conversationId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsByUserId(Long userId) {
        log.info("Getting conversations list for user {}", userId);
        
        try {
            // 使用Repository方法获取用户参与的所有会话
            List<Conversation> conversations = conversationRepository.findByParticipantUserId(userId);
            
            // 过滤未删除的会话
            return conversations.stream()
                    .filter(conv -> !conv.isDeleted())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting conversations list for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserInConversation(Long conversationId, Long userId) {
        log.debug("Checking if user {} is in conversation {}", userId, conversationId);
        
        try {
            return conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId).isPresent();
        } catch (Exception e) {
            log.error("Failed to check if user {} is in conversation {}: {}", userId, conversationId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void deleteConversation(Long conversationId, Long userId) {
        // TODO: 实现删除会话逻辑
        // 1. 验证会话权限
        // 2. 软删除会话
        // 3. 发送WebSocket通知
        log.info("Deleting conversation {} by user {}", conversationId, userId);
    }

    @Override
    public Conversation getOrCreatePrivateConversation(Long userId1, Long userId2) {
        log.info("Getting or creating private conversation between users {} and {}", userId1, userId2);
        
        // 1. 查找现有私聊会话
        // 获取两个用户都参与的会话
        List<ConversationMember> user1Conversations = conversationMemberRepository.findByUserId(userId1);
        List<ConversationMember> user2Conversations = conversationMemberRepository.findByUserId(userId2);
        
        // 找到共同的私聊会话
        for (ConversationMember member1 : user1Conversations) {
            for (ConversationMember member2 : user2Conversations) {
                if (member1.getConversationId().equals(member2.getConversationId())) {
                    // 检查是否为私聊会话
                    Conversation conversation = conversationRepository.findById(member1.getConversationId()).orElse(null);
                    if (conversation != null && conversation.getConversationType() == ConversationType.PRIVATE && !conversation.isDeleted()) {
                        log.info("Found existing private conversation {} between users {} and {}", conversation.getId(), userId1, userId2);
                        return conversation;
                    }
                }
            }
        }
        
        // 2. 如果不存在则创建新会话
        Conversation newConversation = Conversation.builder()
                .conversationType(ConversationType.PRIVATE)
                .name(null) // 私聊不需要名称
                .description(null)
                .avatarUrl(null)
                .createdBy(userId1)
                .lastActiveAt(LocalDateTime.now())
                .deleted(false)
                .build();
        
        // 保存会话
        newConversation = conversationRepository.save(newConversation);
        
        // 添加两个用户为会话成员
        ConversationMember member1 = ConversationMember.builder()
                .conversationId(newConversation.getId())
                .userId(userId1)
                .isPinned(false)
                .isArchived(false)
                .isDnd(false)
                .build();
        
        ConversationMember member2 = ConversationMember.builder()
                .conversationId(newConversation.getId())
                .userId(userId2)
                .isPinned(false)
                .isArchived(false)
                .isDnd(false)
                .build();
        
        conversationMemberRepository.save(member1);
        conversationMemberRepository.save(member2);
        
        log.info("Created new private conversation {} between users {} and {}", newConversation.getId(), userId1, userId2);
        return newConversation;
    }

    @Override
    public void updateLastActiveTime(Long conversationId) {
        log.debug("Updating last active time for conversation {}", conversationId);
        
        try {
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation != null && !conversation.isDeleted()) {
                conversation.setLastActiveAt(LocalDateTime.now());
                conversation = conversationRepository.save(conversation);
                log.debug("Updated last active time for conversation {}", conversationId);
                
                // 构建会话DTO并发送WebSocket通知
                try {
                    // 构建会话DTO
                    ConversationDTO conversationDTO = buildConversationDTO(conversation, null);
                    
                    if (conversationDTO != null) {
                        // 发布事件
                        eventPublisher.publishEvent(new ConversationUpdateEvent(
                            this, // 事件源
                            conversationId,
                            conversationDTO,
                            "UPDATE",
                            null // 不排除任何用户
                        ));
                        log.info("已发布会话更新事件: conversationId={}", conversationId);
                    }
                } catch (Exception e) {
                    log.error("发送会话更新WebSocket通知失败: {}", e.getMessage(), e);
                    // 不影响主流程继续执行
                }
            } else {
                log.warn("Conversation {} not found or deleted, cannot update last active time", conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to update last active time for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }

    @Override
    public List<Long> getConversationMemberIds(Long conversationId) {
        log.debug("获取会话{}的所有成员ID", conversationId);
        
        try {
            // 查询会话成员
            List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversationId);
            
            // 提取成员ID
            return members.stream()
                    .map(ConversationMember::getUserId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取会话{}的成员ID失败", conversationId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void muteConversation(Long conversationId, MuteConversationRequest request, Long userId) {
        log.info("Setting DND for conversation {} by user {}, isDnd={}", conversationId, userId, request.getIsMuted());
        
        try {
            // 1. 验证会话权限
            if (!isUserInConversation(conversationId, userId)) {
                log.warn("User {} is not in conversation {}, cannot set DND", userId, conversationId);
                throw new IllegalArgumentException("您不是此会话的成员，无法设置免打扰");
            }
            
            // 2. 获取会话成员记录
            ConversationMemberId id = new ConversationMemberId(conversationId, userId);
            ConversationMember member = conversationMemberRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("会话成员记录不存在"));
            
            // 3. 更新免打扰状态
            member.setIsDnd(request.getIsMuted());
            
            // 4. 保存更新
            conversationMemberRepository.save(member);
            
            log.info("Successfully set DND for conversation {} by user {} to {}", 
                    conversationId, userId, request.getIsMuted());
                    
            // 5. 发送WebSocket通知
            try {
                // 获取会话信息
                Conversation conversation = getConversationById(conversationId);
                
                // 构建会话DTO
                ConversationDTO conversationDTO = buildConversationDTO(conversation, userId);
                
                if (conversationDTO != null) {
                    // 设置免打扰状态
                    conversationDTO.setIsDnd(request.getIsMuted());
                    
                    // 获取会话的所有成员ID
                    List<Long> memberIds = getConversationMemberIds(conversationId);
                    log.info("获取到会话 {} 的所有成员: {}", conversationId, memberIds);
                    
                    // 向所有会话成员广播更新
                    for (Long memberUserId : memberIds) {
                        // 为每个用户创建特定的更新数据
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("conversationId", conversationId);
                        updateData.put("isDnd", request.getIsMuted());
                        updateData.put("userId", memberUserId); // 设置为接收通知的用户ID
                        
                        eventPublisher.publishEvent(new ConversationUpdateEvent(
                            this,
                            conversationId,
                            updateData,
                            "CONVERSATION_DND",
                            null // 由WebSocketService决定只发给特定用户
                        ));
                        
                        log.info("已发布会话免打扰状态事件给用户 {}: conversationId={}, isDnd={}", 
                                memberUserId, conversationId, request.getIsMuted());
                    }
                }
            } catch (Exception e) {
                log.error("发布会话免打扰状态事件失败: {}", e.getMessage(), e);
                // 不影响主流程继续执行
            }
        } catch (Exception e) {
            log.error("Failed to set DND for conversation: conversationId={}, userId={}, error={}", 
                    conversationId, userId, e.getMessage(), e);
            throw e;
        }
    }
}