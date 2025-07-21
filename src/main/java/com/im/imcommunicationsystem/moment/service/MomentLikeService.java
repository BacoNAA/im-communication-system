package com.im.imcommunicationsystem.moment.service;

import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse.UserBriefInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 动态点赞服务接口
 */
public interface MomentLikeService {

    /**
     * 点赞动态
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 是否点赞成功
     */
    boolean likeMoment(Long momentId, Long userId);

    /**
     * 取消点赞
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean unlikeMoment(Long momentId, Long userId);

    /**
     * 获取动态点赞列表
     *
     * @param momentId 动态ID
     * @param pageable 分页信息
     * @return 点赞用户列表
     */
    Page<UserBriefInfo> getMomentLikes(Long momentId, Pageable pageable);

    /**
     * 获取点赞数量
     *
     * @param momentId 动态ID
     * @return 点赞数量
     */
    long getLikeCount(Long momentId);

    /**
     * 检查用户是否已点赞
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    boolean checkUserLiked(Long momentId, Long userId);
    
    /**
     * 获取用户点赞的动态ID列表
     *
     * @param userId 用户ID
     * @return 动态ID列表
     */
    List<Long> getUserLikedMomentIds(Long userId);
} 