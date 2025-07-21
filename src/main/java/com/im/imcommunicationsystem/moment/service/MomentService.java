package com.im.imcommunicationsystem.moment.service;

import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest;
import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse;
import com.im.imcommunicationsystem.moment.dto.response.MomentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 动态服务接口
 */
public interface MomentService {

    /**
     * 创建新动态
     *
     * @param userId 用户ID
     * @param request 创建动态请求
     * @return 创建的动态响应
     */
    MomentResponse createMoment(Long userId, CreateMomentRequest request);

    /**
     * 获取用户动态时间线
     *
     * @param userId 用户ID
     * @param pageable 分页信息
     * @return 动态分页数据
     */
    Page<MomentResponse> getUserTimeline(Long userId, Pageable pageable);

    /**
     * 获取好友动态时间线
     *
     * @param userId 当前用户ID
     * @param pageable 分页信息
     * @return 动态分页数据
     */
    Page<MomentResponse> getFriendTimeline(Long userId, Pageable pageable);

    /**
     * 获取动态详情
     *
     * @param momentId 动态ID
     * @param userId 当前用户ID
     * @return 动态详情响应
     */
    MomentDetailResponse getMomentDetail(Long momentId, Long userId);

    /**
     * 更新动态内容
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @param content 更新的内容
     * @return 更新后的动态响应
     */
    MomentResponse updateMoment(Long momentId, Long userId, String content);

    /**
     * 删除动态
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteMoment(Long momentId, Long userId);

    /**
     * 检查动态可见性权限
     *
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 是否有权限查看
     */
    boolean checkMomentVisibility(Long momentId, Long userId);
} 