package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.UserManagementRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminUserResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.service.AdminUserService;
import com.im.imcommunicationsystem.admin.utils.AdminPermissionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminOperationLogRepository adminOperationLogRepository;
    private final AdminPermissionUtils adminPermissionUtils;

    @Override
    public Page<AdminUserResponse> getUserListWithPagination(Pageable pageable, String keyword, String status) {
        log.info("获取用户列表，关键词：{}，状态：{}", keyword, status);

        // 这里应该调用用户仓库获取用户列表
        // 由于用户模块未完成，这里返回一个模拟的空页面
        return Page.empty(pageable);
    }

    @Override
    public AdminUserResponse getUserDetails(Long userId) {
        log.info("获取用户详情，用户ID：{}", userId);

        // 这里应该调用用户仓库获取用户详情
        // 由于用户模块未完成，这里返回一个模拟的用户
        return AdminUserResponse.builder()
                .userId(userId)
                .username("user_" + userId)
                .email("user_" + userId + "@example.com")
                .status("ACTIVE")
                .isActive(true)
                .registeredAt(LocalDateTime.now().minusDays(30))
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponse manageUser(UserManagementRequest request) {
        log.info("管理用户，用户ID：{}，操作：{}", request.getUserId(), request.getAction());

        // 这里应该调用用户仓库获取用户并更新状态
        // 由于用户模块未完成，这里返回一个模拟的用户
        String newStatus;
        boolean isActive;
        if ("freeze".equals(request.getAction())) {
            newStatus = "FROZEN";
            isActive = false;
        } else if ("unfreeze".equals(request.getAction())) {
            newStatus = "ACTIVE";
            isActive = true;
        } else {
            throw new IllegalArgumentException("不支持的操作: " + request.getAction());
        }

        // 记录管理操作
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(1L) // 假设管理员ID为1
                .operationType("USER_" + request.getAction().toUpperCase())
                .targetType("USER")
                .targetId(request.getUserId())
                .description("用户管理操作: " + request.getAction() + ", 原因: " + request.getReason())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        return AdminUserResponse.builder()
                .userId(request.getUserId())
                .username("user_" + request.getUserId())
                .email("user_" + request.getUserId() + "@example.com")
                .status(newStatus)
                .isActive(isActive)
                .registeredAt(LocalDateTime.now().minusDays(30))
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("删除用户，用户ID：{}", userId);

        // 这里应该调用用户仓库删除用户
        // 由于用户模块未完成，这里只记录操作

        // 记录管理操作
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(1L) // 假设管理员ID为1
                .operationType("USER_DELETE")
                .targetType("USER")
                .targetId(userId)
                .description("删除用户，用户ID: " + userId)
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);
    }
} 