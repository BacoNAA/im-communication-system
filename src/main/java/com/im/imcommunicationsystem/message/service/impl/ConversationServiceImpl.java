package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
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
import com.im.imcommunicationsystem.message.service.ConversationService;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
                .sorted((a, b) -> {
                    // 置顶的会话排在前面
                    if (a.getIsPinned() != b.getIsPinned()) {
                        return a.getIsPinned() ? -1 : 1;
                    }
                    // 按最后活跃时间倒序排列
                    return b.getLastActiveAt().compareTo(a.getLastActiveAt());
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

            // 获取未读消息数量
            // TODO: 实现获取未读消息数量逻辑
            dto.setUnreadCount(0);

            // 获取参与者数量
            dto.setParticipantCount(participants.size());
            
            log.info("成功构建会话DTO: conversationId={}, participantCount={}", 
                    conversation.getId(), participants.size());
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
        // TODO: 实现置顶会话逻辑
        // 1. 验证会话权限
        // 2. 更新置顶状态
        log.info("Pinning conversation {} by user {}", conversationId, userId);
    }

    @Override
    public void archiveConversation(Long conversationId, ArchiveConversationRequest request, Long userId) {
        // TODO: 实现归档会话逻辑
        // 1. 验证会话权限
        // 2. 更新归档状态
        log.info("Archiving conversation {} by user {}", conversationId, userId);
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
                conversationRepository.save(conversation);
                log.debug("Updated last active time for conversation {}", conversationId);
            } else {
                log.warn("Conversation {} not found or deleted, cannot update last active time", conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to update last active time for conversation {}: {}", conversationId, e.getMessage(), e);
        }
    }
}