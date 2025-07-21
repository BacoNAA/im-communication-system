package com.im.imcommunicationsystem.moment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest.VisibilityRules;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import com.im.imcommunicationsystem.moment.exception.MomentNotFoundException;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import com.im.imcommunicationsystem.moment.service.MomentVisibilityService;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 动态可见性服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomentVisibilityServiceImpl implements MomentVisibilityService {

    private final MomentRepository momentRepository;
    private final ContactService contactService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean setMomentVisibility(Long momentId, Long userId, VisibilityType visibilityType, VisibilityRules visibilityRules) {
        log.info("设置动态可见性: momentId={}, userId={}, visibilityType={}", momentId, userId, visibilityType);
        
        try {
            // 查找动态并验证所有权
            Optional<Moment> momentOpt = momentRepository.findByIdAndUserId(momentId, userId);
            if (momentOpt.isEmpty()) {
                log.warn("设置动态可见性失败: 动态不存在或不属于当前用户, momentId={}, userId={}", momentId, userId);
                return false;
            }
            
            Moment moment = momentOpt.get();
            
            // 设置可见性类型
            moment.setVisibilityType(visibilityType);
            
            // 处理可见性规则
            if (visibilityType == VisibilityType.CUSTOM && visibilityRules != null) {
                try {
                    // 将可见性规则转换为JSON字符串
                    String visibilityRulesJson = objectMapper.writeValueAsString(visibilityRules);
                    moment.setVisibilityRules(visibilityRulesJson);
                    
                    log.info("设置自定义可见性规则: momentId={}, allowedCount={}, blockedCount={}",
                            momentId,
                            visibilityRules.getAllowedUserIds() != null ? visibilityRules.getAllowedUserIds().size() : 0,
                            visibilityRules.getBlockedUserIds() != null ? visibilityRules.getBlockedUserIds().size() : 0);
                } catch (JsonProcessingException e) {
                    log.error("序列化可见性规则失败: momentId={}, error={}", momentId, e.getMessage(), e);
                    return false;
                }
            } else {
                // 非自定义可见性类型，清空规则
                moment.setVisibilityRules(null);
            }
            
            // 保存更新
            momentRepository.save(moment);
            
            log.info("动态可见性设置成功: momentId={}, visibilityType={}", momentId, visibilityType);
            return true;
        } catch (Exception e) {
            log.error("设置动态可见性失败: momentId={}, error={}", momentId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean checkVisibilityPermission(Long momentId, Long viewerId) {
        log.info("检查动态可见性权限: momentId={}, viewerId={}", momentId, viewerId);
        
        try {
            // 获取动态
            Moment moment = momentRepository.findById(momentId)
                    .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
            
            // 动态作者总是可以查看自己的动态
            if (moment.getUserId().equals(viewerId)) {
                log.info("动态作者查看自己的动态: momentId={}, userId={}", momentId, viewerId);
                return true;
            }
            
            // 根据可见性类型判断
            switch (moment.getVisibilityType()) {
                case PUBLIC:
                    // 公开动态需要是好友关系才能查看
                    boolean isFriend = contactService.isFriend(moment.getUserId(), viewerId);
                    log.info("公开动态可见性检查: momentId={}, viewerId={}, isFriend={}", momentId, viewerId, isFriend);
                    return isFriend;
                    
                case PRIVATE:
                    // 私密动态仅作者可见
                    log.info("私密动态仅作者可见: momentId={}, viewerId={}", momentId, viewerId);
                    return false;
                    
                case CUSTOM:
                    // 自定义可见性需要解析规则
                    if (moment.getVisibilityRules() == null) {
                        log.warn("自定义可见性规则为空: momentId={}", momentId);
                        return false;
                    }
                    
                    try {
                        // 解析可见性规则
                        VisibilityRules rules = objectMapper.readValue(
                                moment.getVisibilityRules(),
                                VisibilityRules.class
                        );
                        
                        // 检查是否是好友
                        boolean isFriendForCustom = contactService.isFriend(moment.getUserId(), viewerId);
                        if (!isFriendForCustom) {
                            log.info("非好友用户无法查看自定义可见性动态: momentId={}, viewerId={}", momentId, viewerId);
                            return false;
                        }
                        
                        // 检查是否在允许列表中
                        if (rules.getAllowedUserIds() != null && !rules.getAllowedUserIds().isEmpty()) {
                            boolean isAllowed = rules.getAllowedUserIds().contains(viewerId);
                            log.info("检查用户是否在允许列表: momentId={}, viewerId={}, isAllowed={}", momentId, viewerId, isAllowed);
                            return isAllowed;
                        }
                        
                        // 检查是否在阻止列表中
                        if (rules.getBlockedUserIds() != null && !rules.getBlockedUserIds().isEmpty()) {
                            boolean isBlocked = rules.getBlockedUserIds().contains(viewerId);
                            log.info("检查用户是否在阻止列表: momentId={}, viewerId={}, isBlocked={}", momentId, viewerId, isBlocked);
                            return !isBlocked;
                        }
                        
                        // 默认情况下，好友可见
                        return true;
                        
                    } catch (Exception e) {
                        log.error("解析可见性规则失败: momentId={}, error={}", momentId, e.getMessage(), e);
                        return false;
                    }
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("检查动态可见性权限失败: momentId={}, viewerId={}, error={}", 
                    momentId, viewerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Long> getVisibleMoments(Long userId, List<Long> momentIds) {
        log.info("获取用户可见的动态ID列表: userId={}, momentIds.size={}", userId, momentIds.size());
        
        if (momentIds == null || momentIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 过滤出用户有权限查看的动态
            List<Long> visibleMomentIds = momentIds.stream()
                    .filter(momentId -> checkVisibilityPermission(momentId, userId))
                    .collect(Collectors.toList());
            
            log.info("用户可见的动态数量: userId={}, visible={}, total={}", 
                    userId, visibleMomentIds.size(), momentIds.size());
            
            return visibleMomentIds;
        } catch (Exception e) {
            log.error("获取可见动态列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Long> filterUsersByVisibility(Long userId, List<Long> userIds) {
        log.info("根据可见性过滤用户列表: userId={}, userIds.size={}", userId, userIds.size());
        
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 过滤出与当前用户互为好友的用户ID
            List<Long> friendUserIds = userIds.stream()
                    .filter(friendId -> contactService.isFriend(userId, friendId))
                    .collect(Collectors.toList());
            
            log.info("过滤后的好友用户数量: userId={}, friends={}, total={}", 
                    userId, friendUserIds.size(), userIds.size());
            
            return friendUserIds;
        } catch (Exception e) {
            log.error("根据可见性过滤用户列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
} 