package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.JoinGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupJoinRequestResponse;
import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 群组加入请求服务接口
 */
public interface GroupJoinRequestService {
    
    /**
     * 申请加入群组
     * 
     * @param request 加入群组请求
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean applyToJoinGroup(JoinGroupRequest request, Long userId);
    
    /**
     * 处理群组加入请求
     * 
     * @param requestId 请求ID
     * @param groupId 群组ID
     * @param handlerId 处理者ID
     * @param approve 是否批准
     * @return 是否成功
     */
    boolean handleJoinRequest(Long requestId, Long groupId, Long handlerId, boolean approve);
    
    /**
     * 取消加入请求
     * 
     * @param requestId 请求ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean cancelJoinRequest(Long requestId, Long userId);
    
    /**
     * 获取群组的加入请求
     * 
     * @param groupId 群组ID
     * @param status 请求状态（可选）
     * @param adminId 管理员ID
     * @param pageable 分页参数
     * @return 群组加入请求分页
     */
    Page<GroupJoinRequestResponse> getGroupJoinRequests(
            Long groupId, GroupJoinRequestStatus status, Long adminId, Pageable pageable);
    
    /**
     * 获取用户的加入请求
     * 
     * @param userId 用户ID
     * @param status 请求状态（可选）
     * @param pageable 分页参数
     * @return 用户加入请求分页
     */
    Page<GroupJoinRequestResponse> getUserJoinRequests(
            Long userId, GroupJoinRequestStatus status, Pageable pageable);
    
    /**
     * 获取群组的待处理请求数量
     * 
     * @param groupId 群组ID
     * @return 待处理请求数量
     */
    long getPendingRequestCount(Long groupId);
    
    /**
     * 构建加入请求响应对象
     * 
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @return 加入请求响应列表
     */
    List<GroupJoinRequestResponse> buildUserJoinRequestsForGroup(Long groupId, Long userId);
} 