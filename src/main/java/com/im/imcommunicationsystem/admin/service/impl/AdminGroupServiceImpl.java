package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.response.GroupDetailsResponse;
import com.im.imcommunicationsystem.admin.dto.response.GroupMemberResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.service.AdminGroupService;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理员群组管理服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminGroupServiceImpl implements AdminGroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;

    @Override
    public Page<GroupDetailsResponse> getGroups(Pageable pageable, String keyword, String status) {
        log.info("获取群组列表: keyword={}, status={}, pageable={}", keyword, status, pageable);
        
        // 构建动态查询条件
        Specification<Group> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                try {
                    // 尝试将关键词解析为群组ID
                    Long groupId = Long.parseLong(keyword.trim());
                    predicates.add(criteriaBuilder.equal(root.get("id"), groupId));
                } catch (NumberFormatException e) {
                    // 如果不是有效的数字，则按名称搜索
                    predicates.add(criteriaBuilder.like(root.get("name"), "%" + keyword.trim() + "%"));
                }
            }
            
            // 状态过滤
            if (status != null && !status.equalsIgnoreCase("all")) {
                boolean isBanned = status.equalsIgnoreCase("banned");
                predicates.add(criteriaBuilder.equal(root.get("isBanned"), isBanned));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 执行查询
        Page<Group> groupPage = groupRepository.findAll(spec, pageable);
        
        if (groupPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 获取所有群主ID
        List<Long> ownerIds = groupPage.getContent().stream()
                .map(Group::getOwnerId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询群主信息
        Map<Long, User> ownerMap = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // 批量查询群组成员数量
        List<Long> groupIds = groupPage.getContent().stream()
                .map(Group::getId)
                .collect(Collectors.toList());
        
        Map<Long, Long> memberCountMap = groupMemberRepository.countMembersByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
        
        // 转换为响应DTO
        List<GroupDetailsResponse> responseList = groupPage.getContent().stream()
                .map(group -> {
                    User owner = ownerMap.get(group.getOwnerId());
                    String ownerNickname = owner != null ? owner.getNickname() : "未知用户";
                    Integer memberCount = memberCountMap.containsKey(group.getId()) 
                            ? memberCountMap.get(group.getId()).intValue() : 0;
                    
                    return GroupDetailsResponse.fromEntity(group, memberCount, ownerNickname);
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(responseList, pageable, groupPage.getTotalElements());
    }

    @Override
    public GroupDetailsResponse getGroupDetails(Long groupId) {
        log.info("获取群组详情: groupId={}", groupId);
        
        // 查询群组
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群组不存在: " + groupId));
        
        // 查询群主信息
        User owner = userRepository.findById(group.getOwnerId())
                .orElse(null);
        String ownerNickname = owner != null ? owner.getNickname() : "未知用户";
        
        // 查询成员数量
        Long memberCount = groupMemberRepository.countByIdGroupId(groupId);
        
        // 构建响应
        return GroupDetailsResponse.fromEntity(group, memberCount.intValue(), ownerNickname);
    }

    @Override
    @Transactional
    public boolean manageGroup(Long adminId, Long groupId, String action, String reason, Integer duration) {
        log.info("管理群组: adminId={}, groupId={}, action={}, reason={}, duration={}", 
                adminId, groupId, action, reason, duration);
        
        try {
            // 查询群组
            Optional<Group> groupOptional = groupRepository.findById(groupId);
            if (groupOptional.isEmpty()) {
                log.warn("群组不存在: {}", groupId);
                return false;
            }
            
            Group group = groupOptional.get();
            boolean isSuccess = false;
            
            // 执行相应操作
            switch (action.toLowerCase()) {
                case "ban":
                    isSuccess = banGroup(group, reason, duration);
                    break;
                case "unban":
                    isSuccess = unbanGroup(group, reason);
                    break;
                case "dissolve":
                    isSuccess = dissolveGroup(group, reason);
                    break;
                default:
                    log.warn("不支持的操作: {}", action);
                    return false;
            }
            
            if (isSuccess) {
                // 记录管理员操作日志
                logAdminOperation(adminId, groupId, action, reason);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("管理群组失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 封禁群组
     */
    private boolean banGroup(Group group, String reason, Integer duration) {
        // 如果已经被封禁，则不重复封禁
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            log.info("群组已被封禁，更新封禁信息: groupId={}", group.getId());
        }
        
        // 设置封禁状态
        group.setIsBanned(true);
        group.setBannedReason(reason);
        
        // 设置封禁时长
        if (duration != null && duration > 0) {
            group.setBannedUntil(LocalDateTime.now().plusHours(duration));
        } else {
            group.setBannedUntil(null); // 永久封禁
        }
        
        groupRepository.save(group);
        log.info("群组已封禁: groupId={}, reason={}, duration={}", group.getId(), reason, duration);
        
        return true;
    }
    
    /**
     * 解封群组
     */
    private boolean unbanGroup(Group group, String reason) {
        // 如果未被封禁，则无需操作
        if (Boolean.FALSE.equals(group.getIsBanned())) {
            log.info("群组未被封禁，无需解封: groupId={}", group.getId());
            return true;
        }
        
        // 解除封禁状态
        group.setIsBanned(false);
        group.setBannedReason(null);
        group.setBannedUntil(null);
        
        groupRepository.save(group);
        log.info("群组已解封: groupId={}, reason={}", group.getId(), reason);
        
        return true;
    }
    
    /**
     * 解散群组
     */
    private boolean dissolveGroup(Group group, String reason) {
        try {
            // 先删除所有成员
            groupMemberRepository.deleteAllByIdGroupId(group.getId());
            
            // 再删除群组
            groupRepository.delete(group);
            
            log.info("群组已解散: groupId={}, reason={}", group.getId(), reason);
            return true;
        } catch (Exception e) {
            log.error("解散群组失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 记录管理员操作日志
     */
    private void logAdminOperation(Long adminId, Long groupId, String action, String reason) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setTargetId(groupId);
            log.setDescription(String.format("群组管理：%s，原因：%s", action, reason));
            log.setOperationTypeString(action.toUpperCase());
            log.setTargetTypeString("GROUP");
            log.setCreatedAt(LocalDateTime.now());
            
            adminOperationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录管理员操作日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<GroupMemberResponse> getGroupMembers(Long groupId, Pageable pageable) {
        log.info("获取群组成员列表: groupId={}", groupId);
        
        // 查询群组成员
        Page<GroupMember> memberPage = groupMemberRepository.findByIdGroupId(groupId, pageable);
        
        if (memberPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 获取所有用户ID
        List<Long> userIds = memberPage.getContent().stream()
                .map(member -> member.getId().getUserId())
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // 转换为响应DTO
        List<GroupMemberResponse> responseList = memberPage.getContent().stream()
                .map(member -> {
                    Long userId = member.getId().getUserId();
                    User user = userMap.get(userId);
                    
                    String nickname = user != null ? user.getNickname() : "未知用户";
                    String email = user != null ? user.getEmail() : null;
                    String avatarUrl = user != null ? user.getAvatarUrl() : null;
                    
                    return GroupMemberResponse.fromEntity(member, nickname, email, avatarUrl);
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(responseList, pageable, memberPage.getTotalElements());
    }
} 