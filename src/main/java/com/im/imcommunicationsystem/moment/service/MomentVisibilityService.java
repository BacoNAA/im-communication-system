package com.im.imcommunicationsystem.moment.service;

import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest.VisibilityRules;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;

import java.util.List;

/**
 * 动态可见性服务接口
 */
public interface MomentVisibilityService {

    /**
     * 设置动态可见性
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @param visibilityType 可见性类型
     * @param visibilityRules 可见性规则
     * @return 是否设置成功
     */
    boolean setMomentVisibility(Long momentId, Long userId, VisibilityType visibilityType, VisibilityRules visibilityRules);

    /**
     * 检查可见性权限
     *
     * @param momentId 动态ID
     * @param viewerId 查看者ID
     * @return 是否有权限查看
     */
    boolean checkVisibilityPermission(Long momentId, Long viewerId);

    /**
     * 获取用户可见的动态ID列表
     *
     * @param userId 用户ID
     * @param momentIds 动态ID列表
     * @return 可见的动态ID列表
     */
    List<Long> getVisibleMoments(Long userId, List<Long> momentIds);

    /**
     * 根据可见性过滤动态
     *
     * @param userId 当前用户ID
     * @param userIds 好友用户ID列表
     * @return 可见用户ID列表
     */
    List<Long> filterUsersByVisibility(Long userId, List<Long> userIds);
} 