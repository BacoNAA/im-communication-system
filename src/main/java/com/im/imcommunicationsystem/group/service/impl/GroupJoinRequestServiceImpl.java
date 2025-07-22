package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.group.dto.request.JoinGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupJoinRequestResponse;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.entity.GroupJoinRequest;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.entity.GroupMemberId;
import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import com.im.imcommunicationsystem.group.repository.GroupJoinRequestRepository;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.group.service.GroupJoinRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 群组加入请求服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupJoinRequestServiceImpl implements GroupJoinRequestService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean applyToJoinGroup(JoinGroupRequest request, Long userId) {
        log.info("申请加入群组: groupId={}, userId={}", request.getGroupId(), userId);
        
        // 验证群组是否存在
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("群组不存在"));
        
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 检查用户是否已经是群成员
        boolean isMember = false;
        try {
            isMember = groupMemberRepository.findByIdGroupIdAndIdUserId(group.getId(), userId).isPresent();
        } catch (Exception e) {
            log.warn("检查用户是否为群成员时出错: {}", e.getMessage());
        }
        
        if (isMember) {
            throw new BusinessException("您已经是该群组成员");
        }
        
        // 检查是否已有待处理的加入请求
        if (joinRequestRepository.existsByGroupIdAndUserIdAndStatus(
                group.getId(), userId, GroupJoinRequestStatus.PENDING)) {
            throw new BusinessException("您已发送过加入请求，请等待管理员审批");
        }
        
        // 创建加入请求
        GroupJoinRequest joinRequest = new GroupJoinRequest();
        joinRequest.setGroupId(group.getId());
        joinRequest.setUserId(userId);
        joinRequest.setMessage(StringUtils.hasText(request.getMessage()) ? 
                request.getMessage() : "请求加入群组");
        joinRequest.setStatus(GroupJoinRequestStatus.PENDING);
        joinRequest.setCreatedAt(LocalDateTime.now());
        
        // 如果群组不需要审批，直接添加为成员
        if (!group.getRequiresApproval()) {
            // 创建群成员
            GroupMember groupMember = new GroupMember();
            groupMember.setId(new GroupMemberId(group.getId(), userId));
            groupMember.setRole(GroupMemberRole.member);
            groupMember.setIsMuted(false);
            groupMember.setJoinedAt(LocalDateTime.now());
            
            // 保存群成员
            groupMemberRepository.save(groupMember);
            
            // 更新加入请求状态
            joinRequest.setStatus(GroupJoinRequestStatus.ACCEPTED);
            joinRequest.setHandledAt(LocalDateTime.now());
            joinRequest.setHandlerId(group.getOwnerId()); // 系统自动处理，使用群主ID
        }
        
        // 保存加入请求
        joinRequestRepository.save(joinRequest);
        
        log.info("申请加入群组成功: groupId={}, userId={}, requiresApproval={}, requestId={}",
                group.getId(), userId, group.getRequiresApproval(), joinRequest.getId());
        
        return true;
    }

    @Override
    @Transactional
    public boolean handleJoinRequest(Long requestId, Long groupId, Long handlerId, boolean approve) {
        log.info("处理群组加入请求: requestId={}, groupId={}, handlerId={}, approve={}",
                requestId, groupId, handlerId, approve);
        
        // 获取加入请求
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("加入请求不存在"));
        
        // 验证请求是否对应指定的群组
        if (!request.getGroupId().equals(groupId)) {
            throw new BusinessException("加入请求与群组不匹配");
        }
        
        // 验证请求是否待处理
        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new BusinessException("该请求已被处理");
        }
        
        // 验证操作者是否为群组管理员或群主
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
        
        // 检查群组是否被封禁
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new BusinessException("该群组已被封禁，无法处理加入请求" + reason);
        }
        
        boolean isAdmin = false;
        if (group.getOwnerId().equals(handlerId)) {
            isAdmin = true; // 群主
        } else {
            // 检查是否为管理员
            try {
                Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, handlerId);
                if (memberOpt.isPresent() && memberOpt.get().getRole() == GroupMemberRole.admin) {
                    isAdmin = true;
                }
            } catch (Exception e) {
                log.warn("检查用户是否为群组管理员时出错: {}", e.getMessage());
            }
        }
        
        if (!isAdmin) {
            throw new BusinessException("您没有权限处理此请求");
        }
        
        // 处理请求
        if (approve) {
            // 批准加入
            
            // 检查用户是否已经是群成员
            boolean isMember = false;
            try {
                isMember = groupMemberRepository.findByIdGroupIdAndIdUserId(
                        request.getGroupId(), request.getUserId()).isPresent();
            } catch (Exception e) {
                log.warn("检查用户是否为群成员时出错: {}", e.getMessage());
            }
            
            if (!isMember) {
                // 创建群成员
                GroupMember groupMember = new GroupMember();
                groupMember.setId(new GroupMemberId(request.getGroupId(), request.getUserId()));
                groupMember.setRole(GroupMemberRole.member);
                groupMember.setIsMuted(false);
                groupMember.setJoinedAt(LocalDateTime.now());
                
                // 保存群成员
                groupMemberRepository.save(groupMember);
            }
            
            // 更新请求状态
            request.accept(handlerId);
        } else {
            // 拒绝加入
            request.reject(handlerId);
        }
        
        // 保存请求
        joinRequestRepository.save(request);
        
        log.info("处理群组加入请求成功: requestId={}, approve={}", requestId, approve);
        
        return true;
    }

    @Override
    @Transactional
    public boolean cancelJoinRequest(Long requestId, Long userId) {
        log.info("取消加入请求: requestId={}, userId={}", requestId, userId);
        
        // 获取加入请求
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("加入请求不存在"));
        
        // 验证请求是否由当前用户发起
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException("您没有权限取消此请求");
        }
        
        // 验证请求是否待处理
        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new BusinessException("该请求已被处理，无法取消");
        }
        
        // 取消请求
        request.cancel();
        
        // 保存请求
        joinRequestRepository.save(request);
        
        log.info("取消加入请求成功: requestId={}", requestId);
        
        return true;
    }

    @Override
    public Page<GroupJoinRequestResponse> getGroupJoinRequests(Long groupId, GroupJoinRequestStatus status, Long adminId, Pageable pageable) {
        log.info("获取群组加入请求: groupId={}, status={}, adminId={}", groupId, status, adminId);
        
        // 验证群组是否存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
        
        // 验证操作者权限
        boolean isAdmin = false;
        if (group.getOwnerId().equals(adminId)) {
            isAdmin = true; // 群主
        } else {
            // 检查是否为管理员
            try {
                Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, adminId);
                if (memberOpt.isPresent() && memberOpt.get().getRole() == GroupMemberRole.admin) {
                    isAdmin = true;
                }
            } catch (Exception e) {
                log.warn("检查用户是否为群组管理员时出错: {}", e.getMessage());
            }
        }
        
        if (!isAdmin) {
            throw new BusinessException("您没有权限查看加入请求");
        }
        
        // 查询加入请求
        Page<GroupJoinRequest> requestsPage;
        if (status != null) {
            requestsPage = joinRequestRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(
                    groupId, status, pageable);
        } else {
            // 如果没有指定状态，则查询当前群组的所有请求
            requestsPage = joinRequestRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        }
        
        // 构建响应
        List<GroupJoinRequestResponse> responses = requestsPage.getContent().stream()
                .map(request -> buildJoinRequestResponse(request, adminId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, requestsPage.getTotalElements());
    }

    @Override
    public Page<GroupJoinRequestResponse> getUserJoinRequests(Long userId, GroupJoinRequestStatus status, Pageable pageable) {
        log.info("获取用户加入请求: userId={}, status={}", userId, status);
        
        // 查询加入请求
        Page<GroupJoinRequest> requestsPage;
        if (status != null) {
            requestsPage = joinRequestRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, status, pageable);
        } else {
            // 如果没有指定状态，则查询全部
            requestsPage = joinRequestRepository.findAll(pageable);
        }
        
        // 构建响应
        List<GroupJoinRequestResponse> responses = requestsPage.getContent().stream()
                .map(request -> buildJoinRequestResponse(request, userId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, requestsPage.getTotalElements());
    }

    @Override
    public long getPendingRequestCount(Long groupId) {
        return joinRequestRepository.countByGroupIdAndStatus(groupId, GroupJoinRequestStatus.PENDING);
    }

    @Override
    public List<GroupJoinRequestResponse> buildUserJoinRequestsForGroup(Long groupId, Long userId) {
        log.info("构建用户对群组的加入请求响应: groupId={}, userId={}", groupId, userId);
        
        // 查询用户对该群组的请求
        List<GroupJoinRequest> requests = joinRequestRepository.findByGroupIdAndStatus(
                groupId, GroupJoinRequestStatus.PENDING);
        
        // 过滤出用户的请求
        List<GroupJoinRequest> userRequests = requests.stream()
                .filter(request -> request.getUserId().equals(userId))
                .collect(Collectors.toList());
        
        // 构建响应
        return userRequests.stream()
                .map(request -> buildJoinRequestResponse(request, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 构建加入请求响应
     */
    private GroupJoinRequestResponse buildJoinRequestResponse(GroupJoinRequest request, Long currentUserId) {
        // 获取群组信息
        Group group = groupRepository.findById(request.getGroupId()).orElse(null);
        
        // 获取申请者信息
        User requester = userRepository.findById(request.getUserId()).orElse(null);
        
        // 获取处理者信息
        User handler = null;
        if (request.getHandlerId() != null) {
            handler = userRepository.findById(request.getHandlerId()).orElse(null);
        }
        
        // 判断当前用户权限
        boolean canHandle = false;
        boolean canCancel = false;
        
        if (group != null) {
            // 检查是否为管理员或群主
            if (group.getOwnerId().equals(currentUserId)) {
                canHandle = true;
            } else {
                try {
                    Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(
                            request.getGroupId(), currentUserId);
                    if (memberOpt.isPresent() && memberOpt.get().getRole() == GroupMemberRole.admin) {
                        canHandle = true;
                    }
                } catch (Exception e) {
                    log.warn("检查用户是否为群组管理员时出错: {}", e.getMessage());
                }
            }
        }
        
        // 检查是否为申请者
        if (request.getUserId().equals(currentUserId) && request.getStatus() == GroupJoinRequestStatus.PENDING) {
            canCancel = true;
        }
        
        // 构建状态描述
        String statusDescription;
        switch (request.getStatus()) {
            case PENDING:
                statusDescription = "待处理";
                break;
            case ACCEPTED:
                statusDescription = "已接受";
                break;
            case REJECTED:
                statusDescription = "已拒绝";
                break;
            case CANCELLED:
                statusDescription = "已取消";
                break;
            default:
                statusDescription = "未知状态";
        }
        
        // 构建响应
        GroupJoinRequestResponse response = new GroupJoinRequestResponse();
        response.setId(request.getId());
        response.setGroupId(request.getGroupId());
        
        if (group != null) {
            response.setGroupName(group.getName());
            response.setGroupAvatarUrl(group.getAvatarUrl());
            response.setGroupDescription(group.getDescription());
            
            // 获取成员数量
            long memberCountLong = groupMemberRepository.countByIdGroupId(group.getId());
            response.setGroupMemberCount((int) memberCountLong);
        }
        
        response.setUserId(request.getUserId());
        
        if (requester != null) {
            response.setUsername(requester.getEmail());
            response.setNickname(requester.getNickname());
            response.setAvatarUrl(requester.getAvatarUrl());
        }
        
        response.setMessage(request.getMessage());
        response.setStatus(request.getStatus());
        response.setStatusDescription(statusDescription);
        response.setCreatedAt(request.getCreatedAt());
        response.setHandledAt(request.getHandledAt());
        
        if (request.getHandlerId() != null) {
            response.setHandlerId(request.getHandlerId());
            
            if (handler != null) {
                response.setHandlerUsername(handler.getEmail());
                response.setHandlerNickname(handler.getNickname());
            }
        }
        
        response.setCanHandle(canHandle);
        response.setCanCancel(canCancel);
        
        return response;
    }
} 