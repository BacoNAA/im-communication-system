package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.UserManagementRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminUserResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.enums.OperationType;
import com.im.imcommunicationsystem.admin.enums.TargetType;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.service.AdminUserService;
import com.im.imcommunicationsystem.admin.utils.AdminPermissionUtils;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminOperationLogRepository adminOperationLogRepository;
    private final AdminPermissionUtils adminPermissionUtils;
    private final UserRepository userRepository;

    @Override
    public Page<AdminUserResponse> getUserListWithPagination(Pageable pageable, String keyword, String status) {
        log.info("获取用户列表，关键词：{}，状态：{}", keyword, status);

        // 构建基本查询
        List<User> users;
        long totalCount;

        // 简化实现：直接获取所有用户，然后在内存中过滤
        List<User> allUsers = userRepository.findAll();
        List<User> filteredUsers = allUsers.stream()
            .filter(user -> {
                // 关键词过滤
                boolean matchesKeyword = true;
                if (StringUtils.hasText(keyword)) {
                    String searchTerm = keyword.toLowerCase();
                    matchesKeyword = (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm)) ||
                                     (user.getNickname() != null && user.getNickname().toLowerCase().contains(searchTerm)) ||
                                     (user.getUserIdStr() != null && user.getUserIdStr().toLowerCase().contains(searchTerm));
                }
                
                // 状态过滤
                boolean matchesStatus = true;
                if (StringUtils.hasText(status)) {
                    if ("banned".equals(status)) {
                        matchesStatus = Boolean.TRUE.equals(user.getIsBanned());
                    } else if ("active".equals(status)) {
                        matchesStatus = !Boolean.TRUE.equals(user.getIsBanned());
                    }
                }
                
                return matchesKeyword && matchesStatus;
            })
            .collect(Collectors.toList());
        
        totalCount = filteredUsers.size();
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        
        if (start > filteredUsers.size()) {
            users = new ArrayList<>();
        } else {
            users = filteredUsers.subList(start, end);
        }

        // 转换为DTO并返回分页结果
        List<AdminUserResponse> dtoList = users.stream()
            .map(this::convertToAdminUserResponse)
            .collect(Collectors.toList());
            
        return new PageImpl<>(dtoList, pageable, totalCount);
    }

    @Override
    public AdminUserResponse getUserDetails(Long userId) {
        log.info("获取用户详情，用户ID：{}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));

        return convertToAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse manageUser(UserManagementRequest request) {
        log.info("管理用户，用户ID：{}，操作：{}", request.getUserId(), request.getAction());

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + request.getUserId()));

        // 执行对应操作
        switch (request.getAction()) {
            case "ban":
                user.setIsBanned(true);
                user.setBannedReason(request.getReason());
                if (request.getDuration() != null && request.getDuration() > 0) {
                    user.setBannedUntil(LocalDateTime.now().plusHours(request.getDuration()));
        } else {
                    user.setBannedUntil(null); // 永久封禁
                }
                break;
                
            case "unban":
                user.setIsBanned(false);
                user.setBannedReason(null);
                user.setBannedUntil(null);
                break;
                
            default:
            throw new IllegalArgumentException("不支持的操作: " + request.getAction());
        }

        // 保存用户
        User savedUser = userRepository.save(user);

        // 记录管理操作
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(1L) // 假设管理员ID为1
                .targetId(request.getUserId())
                .description(generateOperationDescription(request))
                .createdAt(LocalDateTime.now())
                .build();

        // 设置枚举值
        if ("ban".equals(request.getAction())) {
            log.setOperationType(OperationType.FREEZE_USER); // 使用现有的FREEZE_USER枚举值
        } else if ("unban".equals(request.getAction())) {
            log.setOperationType(OperationType.UNFREEZE_USER); // 使用现有的UNFREEZE_USER枚举值
        }
        log.setTargetType(TargetType.USER);

        adminOperationLogRepository.save(log);

        return convertToAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("删除用户，用户ID：{}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));

        // 这里不实际删除用户，只是标记为封禁状态
        user.setIsBanned(true);
        user.setBannedReason("用户已被管理员删除");
        userRepository.save(user);

        // 记录管理操作
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(1L) // 假设管理员ID为1
                .targetId(userId)
                .description("删除用户，用户ID: " + userId)
                .createdAt(LocalDateTime.now())
                .build();

        // 设置枚举值
        log.setOperationType(OperationType.DELETE);
        log.setTargetType(TargetType.USER);

        adminOperationLogRepository.save(log);
    }
    
    /**
     * 将User实体转换为AdminUserResponse
     */
    private AdminUserResponse convertToAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .userId(user.getId())
                .username(user.getNickname())
                .email(user.getEmail())
                .status(user.getIsBanned() != null && user.getIsBanned() ? "banned" : "active")
                .isActive(user.getIsBanned() == null || !user.getIsBanned())
                .registeredAt(user.getCreatedAt())
                .lastLoginAt(null) // 目前没有最后登录时间字段
                .userIdStr(user.getUserIdStr())
                .avatarUrl(user.getAvatarUrl())
                .freezeReason(user.getBannedReason())
                .freezeEndDate(user.getBannedUntil())
                .build();
    }
    
    /**
     * 生成操作描述
     */
    private String generateOperationDescription(UserManagementRequest request) {
        StringBuilder description = new StringBuilder();
        
        if ("ban".equals(request.getAction())) {
            description.append("封禁用户");
            if (request.getDuration() != null && request.getDuration() > 0) {
                description.append("，时长: ").append(request.getDuration()).append("小时");
            } else {
                description.append("（永久）");
            }
        } else if ("unban".equals(request.getAction())) {
            description.append("解除用户封禁");
        }
        
        if (StringUtils.hasText(request.getReason())) {
            description.append("，原因: ").append(request.getReason());
        }
        
        return description.toString();
    }
} 