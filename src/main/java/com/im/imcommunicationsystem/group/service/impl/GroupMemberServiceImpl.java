package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import com.im.imcommunicationsystem.group.dto.response.GroupMemberResponse;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.entity.GroupMemberId;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import com.im.imcommunicationsystem.group.exception.GroupException;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.service.GroupMemberService;
import com.im.imcommunicationsystem.group.service.GroupService;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.repository.ConversationRepository;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * 群成员服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public List<GroupMemberResponse> addMembers(Long groupId, List<Long> userIds, Long operatorId) {
        // 验证操作者权限
        if (!groupService.isGroupAdmin(groupId, operatorId)) {
            throw new BusinessException("没有权限添加成员");
        }

        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 验证用户是否存在
        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new BusinessException("部分用户不存在");
        }

        // 验证成员数量上限
        int currentMemberCount = countGroupMembers(groupId);
        if (currentMemberCount + userIds.size() > 500) {
            throw new BusinessException("群组成员数量不能超过500人");
        }

        // 过滤已经在群组中的用户
        List<Long> existingMemberIds = groupMemberRepository.findByIdGroupId(groupId).stream()
                .map(member -> member.getId().getUserId())
                .collect(Collectors.toList());

        List<Long> newMemberIds = userIds.stream()
                .filter(userId -> !existingMemberIds.contains(userId))
                .collect(Collectors.toList());

        if (newMemberIds.isEmpty()) {
            throw new BusinessException("所有用户已经是群成员");
        }

        // 添加新成员
        List<GroupMember> newMembers = newMemberIds.stream()
                .map(userId -> GroupMember.builder()
                        .id(new GroupMemberId(groupId, userId))
                        .role(GroupMemberRole.member)
                        .isMuted(false)
                        .build())
                .collect(Collectors.toList());

        groupMemberRepository.saveAll(newMembers);

        // 返回新添加的成员信息
        return newMembers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<GroupMemberResponse> getGroupMembers(Long groupId, Pageable pageable) {
        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 获取群成员
        Page<GroupMember> members = groupMemberRepository.findByIdGroupId(groupId, pageable);
        
        // 转换为响应对象
        List<GroupMemberResponse> responses = members.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, members.getTotalElements());
    }

    @Override
    public List<GroupMemberResponse> getGroupAdmins(Long groupId) {
        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 获取管理员
        List<GroupMember> admins = groupMemberRepository.findByIdGroupIdAndRoleIn(
                groupId, 
                Arrays.asList(GroupMemberRole.owner, GroupMemberRole.admin)
        );

        // 转换为响应对象
        return admins.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean removeMember(Long groupId, Long userId, Long operatorId) {
        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 验证操作权限
        if (operatorId.equals(userId)) {
            // 自己退出群组
            if (groupService.isGroupOwner(groupId, userId)) {
                throw new BusinessException("群主不能退出群组，请先转让群主");
            }
        } else {
            // 管理员踢人
            if (!groupService.isGroupAdmin(groupId, operatorId)) {
                throw new BusinessException("没有权限移除成员");
            }

            // 管理员不能踢群主
            if (groupService.isGroupOwner(groupId, userId)) {
                throw new BusinessException("不能移除群主");
            }

            // 普通管理员不能踢其他管理员
            if (groupService.isGroupAdmin(groupId, userId) && !groupService.isGroupOwner(groupId, operatorId)) {
                throw new BusinessException("管理员不能移除其他管理员");
            }
        }

        // 获取被移除成员的信息，用于发送通知
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 获取操作者信息
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException("操作者不存在"));

        // 查找与该群组关联的会话
        Conversation groupConversation = conversationRepository.findByRelatedGroupId(groupId);
        if (groupConversation != null) {
            // 获取当前最新消息ID作为最后可接受的消息ID
            Long lastMessageId = messageRepository.findTopByConversationIdOrderByIdDesc(groupConversation.getId())
                    .map(message -> message.getId())
                    .orElse(null);
            
            log.info("为被移出群聊的用户设置最后可接受消息ID: userId={}, groupId={}, conversationId={}, lastAcceptableMessageId={}", 
                    userId, groupId, groupConversation.getId(), lastMessageId);
            
            // 更新会话成员表中的lastAcceptableMessageId
            if (lastMessageId != null) {
                ConversationMemberId memberId = new ConversationMemberId(groupConversation.getId(), userId);
                Optional<ConversationMember> memberOpt = conversationMemberRepository.findById(memberId);
                
                if (memberOpt.isPresent()) {
                    ConversationMember member = memberOpt.get();
                    member.setLastAcceptableMessageId(lastMessageId);
                    conversationMemberRepository.save(member);
                    log.info("已更新用户{}在会话{}中的最后可接受消息ID为{}", userId, groupConversation.getId(), lastMessageId);
                }
            }
        }

        // 删除成员
        groupMemberRepository.deleteByIdGroupIdAndIdUserId(groupId, userId);
        
        // 准备通知数据
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", userId);
        notificationData.put("username", userToRemove.getEmail());
        notificationData.put("nickname", userToRemove.getNickname());
        notificationData.put("operatorId", operatorId);
        notificationData.put("operatorName", operator.getNickname() != null ? operator.getNickname() : operator.getEmail());
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("isSelfExit", operatorId.equals(userId));
        
        // 发送群组更新通知，包括给被移除的成员
        String updateType = operatorId.equals(userId) ? "MEMBER_LEAVE" : "MEMBER_REMOVED";
        webSocketService.sendGroupUpdate(groupId, notificationData, updateType, null);
        
        log.info("用户{}已{}群组{}", userId, operatorId.equals(userId) ? "退出" : "被移除出", groupId);
        
        return true;
    }

    @Override
    @Transactional
    public boolean setAdmin(Long groupId, Long userId, boolean isAdmin, Long operatorId) {
        log.info("开始设置管理员: groupId={}, userId={}, isAdmin={}, operatorId={}", 
                groupId, userId, isAdmin, operatorId);
        
        try {
        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 验证操作者是否为群主
        if (!groupService.isGroupOwner(groupId, operatorId)) {
                log.warn("操作者不是群主: groupId={}, operatorId={}", groupId, operatorId);
            throw new BusinessException("只有群主才能设置管理员");
        }

        // 验证目标用户是否为群成员
            Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId);
            if (!memberOpt.isPresent()) {
                log.warn("目标用户不是群成员: groupId={}, userId={}", groupId, userId);
                throw new BusinessException("用户不是群成员");
            }
            
            GroupMember member = memberOpt.get();

        // 不能对自己操作
        if (userId.equals(operatorId)) {
                log.warn("不能对自己操作: userId={}, operatorId={}", userId, operatorId);
            throw new BusinessException("不能对自己进行此操作");
        }
            
            // 不能修改群主的角色
            if (member.getRole() == GroupMemberRole.owner) {
                log.warn("不能修改群主的角色: groupId={}, userId={}", groupId, userId);
                throw new BusinessException("不能修改群主的角色");
            }
            
            log.info("当前用户角色: {}, 将设置为: {}", member.getRole(), isAdmin ? GroupMemberRole.admin : GroupMemberRole.member);

        // 设置或取消管理员
            member.setRole(isAdmin ? GroupMemberRole.admin : GroupMemberRole.member);
        groupMemberRepository.save(member);

            log.info("设置管理员成功: groupId={}, userId={}, isAdmin={}", groupId, userId, isAdmin);
        return true;
        } catch (BusinessException e) {
            log.warn("设置管理员业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("设置管理员异常: groupId={}, userId={}, isAdmin={}, error={}", 
                    groupId, userId, isAdmin, e.getMessage(), e);
            throw new BusinessException("设置管理员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean setMemberMuteStatus(Long groupId, Long userId, boolean isMuted, Integer minutes, Long operatorId) {
        log.info("设置成员禁言状态: groupId={}, userId={}, isMuted={}, minutes={}, operatorId={}", 
                groupId, userId, isMuted, minutes, operatorId);
        
        try {
        // 验证群组是否存在
        groupService.getGroupEntityById(groupId);

        // 验证操作权限
        if (!groupService.isGroupAdmin(groupId, operatorId)) {
                log.warn("操作者没有管理员权限: operatorId={}, groupId={}", operatorId, groupId);
            throw new BusinessException("没有权限设置禁言");
        }

        // 验证目标用户是否为群成员
        GroupMember member = getGroupMember(groupId, userId);

        // 管理员不能被普通管理员禁言
        if (member.isAdmin() && !groupService.isGroupOwner(groupId, operatorId)) {
                log.warn("普通管理员尝试禁言管理员: operatorId={}, userId={}", operatorId, userId);
            throw new BusinessException("管理员不能被禁言");
        }
    
            // 获取被禁言/解禁成员的信息，用于发送通知
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 获取操作者信息
            User operator = userRepository.findById(operatorId)
                    .orElseThrow(() -> new BusinessException("操作者不存在"));

        // 设置禁言状态
        member.setIsMuted(isMuted);
        
        // 设置禁言截止时间
            LocalDateTime mutedUntil = null;
        if (isMuted && minutes != null) {
                mutedUntil = LocalDateTime.now().plusMinutes(minutes);
                member.setMutedUntil(mutedUntil);
        } else if (!isMuted) {
            member.setMutedUntil(null);
        }
        
            // 保存更新
        groupMemberRepository.save(member);
            
            // 准备通知数据
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("userId", userId);
            notificationData.put("username", targetUser.getEmail());
            notificationData.put("nickname", targetUser.getNickname());
            notificationData.put("operatorId", operatorId);
            notificationData.put("operatorName", operator.getNickname() != null ? operator.getNickname() : operator.getEmail());
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("isMuted", isMuted);
            
            if (mutedUntil != null) {
                notificationData.put("mutedUntil", mutedUntil.toString());
                notificationData.put("minutes", minutes);
            }
            
            // 发送群组更新通知
            String updateType = isMuted ? "MEMBER_MUTED" : "MEMBER_UNMUTED";
            webSocketService.sendGroupUpdate(groupId, notificationData, updateType, null);
            
            log.info("用户{}已被{}禁言状态: groupId={}, isMuted={}, minutes={}", 
                    userId, isMuted ? "设置" : "解除", groupId, isMuted, minutes);
            
        return true;
        } catch (BusinessException e) {
            log.warn("设置禁言业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("设置禁言异常: groupId={}, userId={}, isMuted={}, error={}", 
                    groupId, userId, isMuted, e.getMessage(), e);
            throw new BusinessException("设置禁言失败: " + e.getMessage());
        }
    }

    @Override
    public GroupMember getGroupMember(Long groupId, Long userId) {
        return groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId)
                .orElseThrow(() -> new GroupException("用户不是群成员"));
    }

    @Override
    public int countGroupMembers(Long groupId) {
        return (int) groupMemberRepository.countByIdGroupId(groupId);
    }

    /**
     * 将实体转换为响应对象
     */
    private GroupMemberResponse convertToResponse(GroupMember member) {
        User user = userRepository.findById(member.getId().getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        return GroupMemberResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole().name())
                .isMuted(member.isMuted())
                .mutedUntil(member.getMutedUntil())
                .joinedAt(member.getJoinedAt())
                .isOnline(false) // 需要从在线状态服务获取
                .build();
    }
} 