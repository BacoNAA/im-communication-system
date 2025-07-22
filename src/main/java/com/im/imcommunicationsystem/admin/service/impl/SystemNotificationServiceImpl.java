package com.im.imcommunicationsystem.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationDetailResponse;
import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationResponse;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import com.im.imcommunicationsystem.admin.entity.SystemNotificationStatus;
import com.im.imcommunicationsystem.admin.repository.SystemNotificationRepository;
import com.im.imcommunicationsystem.admin.repository.SystemNotificationStatusRepository;
import com.im.imcommunicationsystem.admin.service.SystemNotificationService;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.ResourceNotFoundException;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统通知服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationServiceImpl implements SystemNotificationService {

    private final SystemNotificationRepository systemNotificationRepository;
    private final SystemNotificationStatusRepository notificationStatusRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SystemNotification createSystemNotification(Long adminId, SystemNotificationRequest request) {
        log.info("创建系统通知，管理员ID：{}，请求：{}", adminId, request);
        
        try {
            // 构建系统通知实体
            SystemNotification notification = SystemNotification.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .type(request.getType())
                    .createdBy(adminId)
                    .createdAt(LocalDateTime.now())
                    .isPublished(false)
                    .build();
            
            // 设置目标类型
            if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
                notification.setTargetType(SystemNotification.TargetType.specific_users);
                
                // 将目标用户ID列表转换为JSON字符串
                String targetUsersJson = objectMapper.writeValueAsString(request.getTargetUserIds());
                notification.setTargetUsers(targetUsersJson);
            } else {
                notification.setTargetType(SystemNotification.TargetType.all);
                notification.setTargetUsers(null);
            }
            
            // 保存通知
            SystemNotification savedNotification = systemNotificationRepository.save(notification);
            log.info("系统通知创建成功，ID：{}", savedNotification.getId());
            
            return savedNotification;
        } catch (JsonProcessingException e) {
            log.error("处理目标用户JSON失败", e);
            throw new RuntimeException("处理目标用户数据失败", e);
        }
    }

    @Override
    @Transactional
    public SystemNotification updateSystemNotification(Long adminId, Long notificationId, SystemNotificationRequest request) {
        log.info("更新系统通知，管理员ID：{}，通知ID：{}，请求：{}", adminId, notificationId, request);
        
        // 检查通知是否存在
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在：" + notificationId));
        
        // 检查通知是否已发布，已发布的通知不允许修改
        if (Boolean.TRUE.equals(notification.getIsPublished())) {
            throw new IllegalStateException("已发布的通知不允许修改");
        }
        
        try {
            // 更新通知内容
            notification.setTitle(request.getTitle());
            notification.setContent(request.getContent());
            notification.setType(request.getType());
            
            // 更新目标类型
            if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
                notification.setTargetType(SystemNotification.TargetType.specific_users);
                
                // 将目标用户ID列表转换为JSON字符串
                String targetUsersJson = objectMapper.writeValueAsString(request.getTargetUserIds());
                notification.setTargetUsers(targetUsersJson);
            } else {
                notification.setTargetType(SystemNotification.TargetType.all);
                notification.setTargetUsers(null);
            }
            
            // 保存更新后的通知
            SystemNotification updatedNotification = systemNotificationRepository.save(notification);
            log.info("系统通知更新成功，ID：{}", updatedNotification.getId());
            
            return updatedNotification;
        } catch (JsonProcessingException e) {
            log.error("处理目标用户JSON失败", e);
            throw new RuntimeException("处理目标用户数据失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteSystemNotification(Long adminId, Long notificationId) {
        log.info("删除系统通知，管理员ID：{}，通知ID：{}", adminId, notificationId);
        
        // 检查通知是否存在
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在：" + notificationId));
        
        // 删除通知（关联的状态记录会由于外键级联删除而自动删除）
        systemNotificationRepository.delete(notification);
        log.info("系统通知删除成功，ID：{}", notificationId);
    }

    @Override
    public SystemNotification getSystemNotificationById(Long notificationId) {
        log.info("获取系统通知，通知ID：{}", notificationId);
        
        return systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在：" + notificationId));
    }

    @Override
    public Page<SystemNotification> getSystemNotificationsWithPagination(Pageable pageable, String type) {
        log.info("获取系统通知列表，类型：{}", type);
        
        if (StringUtils.hasText(type)) {
            return systemNotificationRepository.findByType(type, pageable);
        } else {
            return systemNotificationRepository.findAll(pageable);
        }
    }

    @Override
    public Page<SystemNotification> getActiveSystemNotifications(Pageable pageable) {
        log.info("获取活跃系统通知列表");
        
        LocalDateTime now = LocalDateTime.now();
        return systemNotificationRepository.findActiveNotifications(now, pageable);
    }

    @Override
    @Transactional
    public SystemNotification publishSystemNotification(Long adminId, Long notificationId) {
        log.info("发布系统通知，管理员ID：{}，通知ID：{}", adminId, notificationId);
        
        // 检查通知是否存在
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在：" + notificationId));
        
        // 检查通知是否已发布
        if (Boolean.TRUE.equals(notification.getIsPublished())) {
            log.warn("通知已经发布，ID：{}", notificationId);
            return notification;
        }
        
        // 更新通知状态为已发布
        notification.setIsPublished(true);
        notification.setPublishedAt(LocalDateTime.now());
        SystemNotification publishedNotification = systemNotificationRepository.save(notification);
        log.info("系统通知发布成功，ID：{}", publishedNotification.getId());
        
        // 为目标用户创建状态记录
        if (notification.getTargetType() == SystemNotification.TargetType.all) {
            // 为所有用户创建状态记录
            createNotificationStatusForAllUsers(notification.getId());
        } else if (notification.getTargetType() == SystemNotification.TargetType.specific_users) {
            // 为特定用户创建状态记录
            try {
                List<Object> rawTargetUserIds = objectMapper.readValue(notification.getTargetUsers(), List.class);
                
                // 将不同类型的用户ID转换为Long类型
                List<Long> targetUserIds = rawTargetUserIds.stream().map(item -> {
                    if (item instanceof Integer) {
                        return ((Integer) item).longValue();
                    } else if (item instanceof Long) {
                        return (Long) item;
                    } else if (item instanceof String) {
                        try {
                            return Long.valueOf((String) item);
                        } catch (NumberFormatException e) {
                            log.warn("无法将字符串转换为Long: {}", item);
                            return null;
                        }
                    } else {
                        log.warn("未知的用户ID类型: {}", item != null ? item.getClass().getName() : "null");
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                
                log.info("转换后的targetUserIds: {}", targetUserIds);
                createNotificationStatusForUsers(notification.getId(), targetUserIds);
            } catch (JsonProcessingException e) {
                log.error("解析目标用户JSON失败", e);
                throw new RuntimeException("解析目标用户数据失败", e);
            }
        }
        
        // 通过WebSocket推送给在线用户
        pushNotificationToOnlineUsers(publishedNotification);
        
        return publishedNotification;
    }

    @Override
    @Transactional
    public void createNotificationStatusForAllUsers(Long notificationId) {
        log.info("为所有用户创建通知状态记录，通知ID：{}", notificationId);
        
        // 获取所有用户
        List<User> allUsers = userRepository.findAll();
        log.info("用户总数：{}", allUsers.size());
        
        // 批量创建状态记录
        List<SystemNotificationStatus> statusList = allUsers.stream()
                .map(user -> SystemNotificationStatus.builder()
                        .notificationId(notificationId)
                        .userId(user.getId())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        
        notificationStatusRepository.saveAll(statusList);
        log.info("为所有用户创建通知状态记录成功，共{}条", statusList.size());
    }

    @Override
    @Transactional
    public void createNotificationStatusForUsers(Long notificationId, List<Long> userIds) {
        log.info("为特定用户创建通知状态记录，通知ID：{}，用户数：{}", notificationId, userIds.size());
        
        // 批量创建状态记录
        List<SystemNotificationStatus> statusList = userIds.stream()
                .map(userId -> SystemNotificationStatus.builder()
                        .notificationId(notificationId)
                        .userId(userId)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        
        notificationStatusRepository.saveAll(statusList);
        log.info("为特定用户创建通知状态记录成功，共{}条", statusList.size());
    }

    @Override
    public void pushNotificationToOnlineUsers(SystemNotification notification) {
        log.info("通过WebSocket推送通知给在线用户，通知ID：{}", notification.getId());
        
        try {
            // 准备通知数据
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("id", notification.getId());
            notificationData.put("title", notification.getTitle());
            notificationData.put("content", notification.getContent());
            notificationData.put("type", notification.getType());
            notificationData.put("publishedAt", notification.getPublishedAt());
            
            // 创建WebSocket消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "SYSTEM_NOTIFICATION");
            message.put("data", notificationData);
            
            // 确定推送目标
            if (notification.getTargetType() == SystemNotification.TargetType.all) {
                // 广播给所有在线用户
                webSocketService.broadcastMessage(message);
                log.info("已向所有在线用户推送通知");
            } else if (notification.getTargetType() == SystemNotification.TargetType.specific_users) {
                try {
                    // 向特定用户推送
                    List<Object> rawTargetUserIds = objectMapper.readValue(notification.getTargetUsers(), List.class);
                    
                    // 将不同类型的用户ID转换为Long类型
                    List<Long> targetUserIds = rawTargetUserIds.stream().map(item -> {
                        if (item instanceof Integer) {
                            return ((Integer) item).longValue();
                        } else if (item instanceof Long) {
                            return (Long) item;
                        } else if (item instanceof String) {
                            try {
                                return Long.valueOf((String) item);
                            } catch (NumberFormatException e) {
                                log.warn("无法将字符串转换为Long: {}", item);
                                return null;
                            }
                        } else {
                            log.warn("未知的用户ID类型: {}", item != null ? item.getClass().getName() : "null");
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    
                    for (Long userId : targetUserIds) {
                        webSocketService.sendMessageToUser(userId, message);
                    }
                    log.info("已向{}个特定用户推送通知", targetUserIds.size());
                } catch (JsonProcessingException e) {
                    log.error("解析目标用户JSON失败", e);
                    throw new RuntimeException("解析目标用户数据失败", e);
                }
            }
        } catch (Exception e) {
            log.error("推送通知失败", e);
            throw new RuntimeException("推送通知失败", e);
        }
    }

    @Override
    public Page<SystemNotificationResponse> getUserNotifications(Long userId, Pageable pageable, boolean unreadOnly) {
        log.info("获取用户通知列表，用户ID：{}，只看未读：{}", userId, unreadOnly);
        
        // 获取用户通知状态记录
        Page<SystemNotificationStatus> statusPage;
        if (unreadOnly) {
            statusPage = notificationStatusRepository.findByUserIdAndIsReadFalse(userId, pageable);
        } else {
            statusPage = notificationStatusRepository.findByUserId(userId, pageable);
        }
        
        // 获取通知ID列表
        List<Long> notificationIds = statusPage.getContent().stream()
                .map(SystemNotificationStatus::getNotificationId)
                .collect(Collectors.toList());
        
        if (notificationIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        // 获取通知详情
        Map<Long, SystemNotification> notificationMap = systemNotificationRepository.findAllById(notificationIds).stream()
                .collect(Collectors.toMap(SystemNotification::getId, notification -> notification));
        
        // 构建响应
        List<SystemNotificationResponse> responseList = statusPage.getContent().stream()
                .map(status -> {
                    SystemNotification notification = notificationMap.get(status.getNotificationId());
                    if (notification == null) {
                        // 通知可能已被删除
        return null;
                    }
                    
                    return buildNotificationResponse(notification, status);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responseList, pageable, statusPage.getTotalElements());
    }

    @Override
    public SystemNotificationDetailResponse getUserNotificationDetail(Long userId, Long notificationId) {
        log.info("获取用户通知详情，用户ID：{}，通知ID：{}", userId, notificationId);
        
        // 获取通知
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在：" + notificationId));
        
        // 获取用户的通知状态
        SystemNotificationStatus status = notificationStatusRepository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("通知状态不存在"));
        
        // 构建响应
        return buildNotificationDetailResponse(notification, status);
    }

    @Override
    @Transactional
    public boolean markNotificationAsRead(Long userId, Long notificationId) {
        log.info("标记通知为已读，用户ID：{}，通知ID：{}", userId, notificationId);
        
        // 更新状态为已读
        int updatedCount = notificationStatusRepository.markAsRead(notificationId, userId);
        
        return updatedCount > 0;
    }

    @Override
    @Transactional
    public int markMultipleNotificationsAsRead(Long userId, List<Long> notificationIds) {
        log.info("标记多个通知为已读，用户ID：{}，通知数：{}", userId, notificationIds.size());
        
        // 更新状态为已读
        return notificationStatusRepository.markMultipleAsRead(notificationIds, userId);
    }

    @Override
    @Transactional
    public int markAllNotificationsAsRead(Long userId) {
        log.info("标记所有通知为已读，用户ID：{}", userId);
        
        // 更新所有状态为已读
        return notificationStatusRepository.markAllAsRead(userId);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        log.info("获取用户未读通知数量，用户ID：{}", userId);
        
        // 查询未读通知数量
        return notificationStatusRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * 构建通知响应
     */
    private SystemNotificationResponse buildNotificationResponse(SystemNotification notification, SystemNotificationStatus status) {
        return SystemNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .summary(generateSummary(notification.getContent()))
                .type(notification.getType())
                .isRead(status.getIsRead())
                .publishedAt(notification.getPublishedAt())
                .createdAt(notification.getCreatedAt())
                .readAt(status.getReadAt())
                .build();
    }
    
    /**
     * 构建通知详情响应
     */
    private SystemNotificationDetailResponse buildNotificationDetailResponse(SystemNotification notification, SystemNotificationStatus status) {
        return SystemNotificationDetailResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(status.getIsRead())
                .publishedAt(notification.getPublishedAt())
                .createdAt(notification.getCreatedAt())
                .readAt(status.getReadAt())
                .createdBy("管理员")  // 可以根据createdBy获取管理员信息
                .build();
    }
    
    /**
     * 生成内容摘要
     */
    private String generateSummary(String content) {
        if (content == null) {
            return "";
        }
        
        // 取前50个字符作为摘要
        int maxLength = 50;
        if (content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength) + "...";
    }
} 