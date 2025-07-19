package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.common.constants.CommonConstants;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import com.im.imcommunicationsystem.group.dto.request.CreateGroupRequest;
import com.im.imcommunicationsystem.group.dto.request.UpdateGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupResponse;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.entity.GroupMemberId;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import com.im.imcommunicationsystem.group.exception.GroupException;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.group.service.GroupService;
import com.im.imcommunicationsystem.group.utils.GroupUtils;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;
import com.im.imcommunicationsystem.message.enums.ConversationType;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.repository.ConversationRepository;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 群组服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final GroupUtils groupUtils;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        log.info("开始创建群组: userId={}, groupName={}", userId, request.getName());
        
        // 验证创建者是否存在
        log.debug("验证创建者是否存在: userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("创建者不存在: userId={}", userId);
                    return new BusinessException("创建者不存在");
                });

        // 验证成员是否存在
        List<Long> memberIds = new ArrayList<>(request.getMemberIds());
        if (!memberIds.contains(userId)) {
            log.debug("添加创建者到成员列表: userId={}", userId);
            memberIds.add(userId); // 确保创建者也是群成员
        }

        // 验证成员数量不超过上限
        log.debug("验证成员数量: count={}, maxAllowed={}", memberIds.size(), CommonConstants.Group.MAX_MEMBERS);
        if (memberIds.size() > CommonConstants.Group.MAX_MEMBERS) {
            log.warn("群组成员数量超过上限: count={}, maxAllowed={}", memberIds.size(), CommonConstants.Group.MAX_MEMBERS);
            throw new BusinessException("群组成员数量不能超过" + CommonConstants.Group.MAX_MEMBERS + "人");
        }

        // 验证所有成员ID是否存在
        log.debug("验证所有成员ID是否存在: memberIds={}", memberIds);
        List<Long> existingUserIds = userRepository.findAllById(memberIds)
                .stream()
                .map(user -> user.getId())
                .collect(Collectors.toList());

        // 找出不存在的用户ID
        List<Long> nonExistingUserIds = memberIds.stream()
                .filter(id -> !existingUserIds.contains(id))
                .collect(Collectors.toList());

        if (!nonExistingUserIds.isEmpty()) {
            log.warn("存在无效的成员ID: nonExistingUserIds={}", nonExistingUserIds);
            throw new BusinessException("以下用户ID不存在: " + nonExistingUserIds);
        }

        try {
            // 创建群组
            log.debug("开始创建群组实体");
            Group group = Group.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : CommonConstants.Group.DEFAULT_AVATAR)
                    .ownerId(userId)
                    .requiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false)
                    .isAllMuted(false)
                    .build();

            group = groupRepository.save(group);
            final Long groupId = group.getId();
            log.info("群组实体创建成功: groupId={}", groupId);

            // 创建对应的会话
            log.debug("开始创建会话实体");
            final Conversation conversation = new Conversation();
            conversation.setConversationType(ConversationType.GROUP);
            conversation.setCreatedBy(userId);
            conversation.setName(group.getName());
            conversation.setAvatarUrl(group.getAvatarUrl());
            conversation.setLastActiveAt(LocalDateTime.now()); // 初始化最后活跃时间
            
            // 设置关联的群组ID
            conversation.setRelatedGroupId(groupId);
            
            conversationRepository.save(conversation);
            log.info("会话实体创建成功: conversationId={}", conversation.getId());

            // 添加群成员
            log.debug("开始添加群成员: memberCount={}", memberIds.size());
            List<GroupMember> groupMembers = memberIds.stream()
                    .map(memberId -> {
                        GroupMemberRole role = memberId.equals(userId) ? 
                                GroupMemberRole.owner : GroupMemberRole.member;
                        
                        return GroupMember.builder()
                                .id(new GroupMemberId(groupId, memberId))
                                .role(role)
                                .isMuted(false)
                                .build();
                    })
                    .collect(Collectors.toList());

            groupMemberRepository.saveAll(groupMembers);
            log.info("群成员添加成功: groupId={}, memberCount={}", groupId, groupMembers.size());

            // 添加会话成员
            log.debug("开始添加会话成员: memberCount={}", memberIds.size());
            List<ConversationMember> conversationMembers = memberIds.stream()
                    .map(memberId -> {
                        return ConversationMember.builder()
                                .conversationId(conversation.getId())
                                .userId(memberId)
                                .isPinned(false)
                                .isArchived(false)
                                .isDnd(false)
                                .build();
                    })
                    .collect(Collectors.toList());

            conversationMemberRepository.saveAll(conversationMembers);
            log.info("会话成员添加成功: conversationId={}, memberCount={}", conversation.getId(), conversationMembers.size());

            // 返回创建结果
            GroupResponse response = buildGroupResponse(group, userId);
            log.info("群组创建完成: groupId={}, groupName={}", groupId, group.getName());
            return response;
        } catch (Exception e) {
            log.error("创建群组过程中发生异常: groupName={}, error={}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GroupResponse getGroupById(Long groupId, Long userId) {
        Group group = getGroupEntityById(groupId);
        return buildGroupResponse(group, userId);
    }

    @Override
    public Page<GroupResponse> getUserGroups(Long userId, Pageable pageable) {
        try {
            log.info("开始获取用户群组: userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
            
            // 检查用户是否存在
            if (!userRepository.existsById(userId)) {
                log.warn("用户不存在: userId={}", userId);
                return Page.empty(pageable);
            }
            
            // 检查用户是否有群组成员记录
            List<GroupMember> userGroupMembers = groupMemberRepository.findByIdUserId(userId);
            log.info("用户的群组成员记录数量: userId={}, count={}", userId, userGroupMembers.size());
            
            if (!userGroupMembers.isEmpty()) {
                log.info("用户的群组成员记录: userId={}, groupIds={}", userId, 
                    userGroupMembers.stream()
                        .map(m -> m.getId().getGroupId())
                        .collect(Collectors.toList()));
            }
            
            // 查询用户参与的群组
            Page<Group> groups = groupRepository.findGroupsByUserId(userId, pageable);
            log.info("成功获取用户群组: userId={}, count={}", userId, groups.getTotalElements());
            
            // 如果没有找到群组，尝试查询用户创建的群组
            if (groups.isEmpty()) {
                log.info("尝试查询用户创建的群组: userId={}", userId);
                groups = groupRepository.findByOwnerId(userId, pageable);
                log.info("用户创建的群组: count={}", groups.getTotalElements());
            }
            
            // 构建响应
            List<GroupResponse> responses = groups.getContent().stream()
                    .map(group -> {
                        try {
                            GroupResponse response = buildGroupResponse(group, userId);
                            log.debug("构建群组响应成功: groupId={}, groupName={}", group.getId(), group.getName());
                            return response;
                        } catch (Exception e) {
                            log.error("构建群组响应对象失败: groupId={}, error={}", group.getId(), e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.info("返回群组响应: userId={}, count={}", userId, responses.size());
            return new PageImpl<>(responses, pageable, groups.getTotalElements());
        } catch (Exception e) {
            log.error("获取用户群组失败: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean dissolveGroup(Long groupId, Long userId) {
        Group group = getGroupEntityById(groupId);
        
        // 验证操作者是否为群主
        if (!isGroupOwner(groupId, userId)) {
            throw new BusinessException("只有群主才能解散群组");
        }
        
        // 删除群组及相关数据
        groupRepository.delete(group);
        return true;
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        return groupRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
    }

    @Override
    public boolean isGroupAdmin(Long groupId, Long userId) {
        Optional<GroupMember> member = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId);
        return member.isPresent() && 
               (member.get().getRole() == GroupMemberRole.admin || member.get().getRole() == GroupMemberRole.owner);
    }

    @Override
    public boolean isGroupOwner(Long groupId, Long userId) {
        Optional<GroupMember> member = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId);
        return member.isPresent() && member.get().getRole() == GroupMemberRole.owner;
    }

    @Override
    public Group getGroupEntityById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("群组不存在"));
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        log.info("开始更新群组: groupId={}, userId={}", groupId, userId);
        
        // 获取群组实体
        Group group = getGroupEntityById(groupId);
        
        // 验证操作者是否有权限修改群组信息
        if (!isGroupAdmin(groupId, userId) && !isGroupOwner(groupId, userId)) {
            log.warn("用户无权更新群组: userId={}, groupId={}", userId, groupId);
            throw new BusinessException("只有群主或管理员才能修改群组信息");
        }
        
        try {
            // 更新群组信息
            boolean changed = false;
            
            if (request.getName() != null && !request.getName().equals(group.getName())) {
                group.setName(request.getName());
                changed = true;
                log.info("更新群组名称: groupId={}, newName={}", groupId, request.getName());
            }
            
            if (request.getDescription() != null && !request.getDescription().equals(group.getDescription())) {
                group.setDescription(request.getDescription());
                changed = true;
                log.info("更新群组介绍: groupId={}", groupId);
            }
            
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(group.getAvatarUrl())) {
                group.setAvatarUrl(request.getAvatarUrl());
                changed = true;
                log.info("更新群组头像: groupId={}", groupId);
            }
            
            if (request.getRequiresApproval() != null && !request.getRequiresApproval().equals(group.getRequiresApproval())) {
                // 只有群主可以修改审批设置
                if (!isGroupOwner(groupId, userId)) {
                    log.warn("非群主尝试修改审批设置: userId={}, groupId={}", userId, groupId);
                    throw new BusinessException("只有群主才能修改审批设置");
                }
                
                group.setRequiresApproval(request.getRequiresApproval());
                changed = true;
                log.info("更新群组审批设置: groupId={}, requiresApproval={}", groupId, request.getRequiresApproval());
            }
            
            // 如果没有任何变化，直接返回
            if (!changed) {
                log.info("群组信息未发生变化: groupId={}", groupId);
                return buildGroupResponse(group, userId);
            }
            
            // 保存更新后的群组信息
            group = groupRepository.save(group);
            final Group finalGroup = group;
            log.info("群组信息更新成功: groupId={}", groupId);
            
            // 查找关联的会话，同步更新会话信息
            // 查找与群组相关的会话
            List<Conversation> conversations = conversationRepository.findAll().stream()
                    .filter(c -> c.getConversationType() == ConversationType.GROUP)
                    .filter(c -> groupId.equals(c.getRelatedGroupId()))
                    .collect(Collectors.toList());
            
            if (!conversations.isEmpty()) {
                log.info("找到与群组{}关联的会话: count={}", groupId, conversations.size());
                
                for (Conversation conversation : conversations) {
                    // 同步更新会话的名称和头像
                    conversation.setName(finalGroup.getName());
                    conversation.setAvatarUrl(finalGroup.getAvatarUrl());
                    
                    conversationRepository.save(conversation);
                    log.info("同步更新会话信息成功: conversationId={}", conversation.getId());
                    
                    // 发送会话更新通知
                    webSocketService.sendConversationUpdate(
                        conversation.getId(),
                        buildConversationUpdateData(conversation),
                        "UPDATE",
                        null
                    );
                }
            } else {
                log.warn("未找到与群组{}关联的会话", groupId);
            }
            
            // 发送群组更新通知
            webSocketService.sendGroupUpdate(
                groupId,
                buildGroupResponse(group, userId),
                "UPDATE",
                null
            );
            
            return buildGroupResponse(group, userId);
        } catch (Exception e) {
            log.error("更新群组信息失败: groupId={}, error={}", groupId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 构建会话更新数据
     */
    private Map<String, Object> buildConversationUpdateData(Conversation conversation) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", conversation.getId());
        data.put("name", conversation.getName());
        data.put("avatarUrl", conversation.getAvatarUrl());
        data.put("conversationType", conversation.getConversationType());
        data.put("lastActiveAt", conversation.getLastActiveAt());
        return data;
    }
    
    /**
     * 从Map中获取Long值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
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
     * 构建群组响应对象
     */
    private GroupResponse buildGroupResponse(Group group, Long currentUserId) {
        // 获取群成员数量
        long memberCount = groupMemberRepository.countByIdGroupId(group.getId());
        
        // 获取当前用户角色
        String userRole = "NON_MEMBER";
        Optional<GroupMember> currentMember = groupMemberRepository.findByIdGroupIdAndIdUserId(group.getId(), currentUserId);
        if (currentMember.isPresent()) {
            GroupMemberRole role = currentMember.get().getRole();
            userRole = role != null ? role.name() : "member";
            log.debug("用户在群组中的角色: userId={}, groupId={}, role={}", currentUserId, group.getId(), userRole);
        }
        
        // 获取群主昵称
        String ownerNickname = userRepository.findById(group.getOwnerId())
                .map(user -> user.getNickname())
                .orElse("未知用户");
        
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .ownerId(group.getOwnerId())
                .ownerNickname(ownerNickname)
                .memberCount((int) memberCount)
                .requiresApproval(group.getRequiresApproval())
                .isAllMuted(group.getIsAllMuted())
                .userRole(userRole)
                .createdAt(group.getCreatedAt())
                .build();
    }
} 