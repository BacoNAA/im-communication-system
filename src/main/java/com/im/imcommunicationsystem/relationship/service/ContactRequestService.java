package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.dto.request.ContactRequestCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactRequestResponse;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;

import java.util.List;

/**
 * 好友请求服务接口
 * 处理好友请求相关的业务逻辑
 */
public interface ContactRequestService {

    /**
     * 发送好友请求
     * @param request 好友请求创建请求
     * @return 是否发送成功
     */
    boolean sendContactRequest(ContactRequestCreateRequest request);

    /**
     * 接受好友请求
     * @param requestId 请求ID
     * @param recipientId 接收者ID
     * @return 是否接受成功
     */
    boolean acceptContactRequest(Long requestId, Long recipientId);

    /**
     * 拒绝好友请求
     * @param requestId 请求ID
     * @param recipientId 接收者ID
     * @return 是否拒绝成功
     */
    boolean rejectContactRequest(Long requestId, Long recipientId);

    /**
     * 撤回好友请求
     * @param requestId 请求ID
     * @param requesterId 请求者ID
     * @return 是否撤回成功
     */
    boolean withdrawContactRequest(Long requestId, Long requesterId);

    /**
     * 获取收到的好友请求列表
     * @param userId 用户ID
     * @param status 请求状态（可选）
     * @return 好友请求响应列表
     */
    List<ContactRequestResponse> getReceivedRequests(Long userId, ContactRequestStatus status);

    /**
     * 获取发送的好友请求列表
     * @param userId 用户ID
     * @param status 请求状态（可选）
     * @return 好友请求响应列表
     */
    List<ContactRequestResponse> getSentRequests(Long userId, ContactRequestStatus status);

    /**
     * 获取好友请求详情
     * @param requestId 请求ID
     * @param userId 用户ID（用于权限验证）
     * @return 好友请求响应
     */
    ContactRequestResponse getRequestDetail(Long requestId, Long userId);

    /**
     * 检查是否存在待处理的好友请求
     * @param requesterId 请求者ID
     * @param recipientId 接收者ID
     * @return 是否存在待处理请求
     */
    boolean hasPendingRequest(Long requesterId, Long recipientId);

    /**
     * 获取待处理请求数量
     * @param userId 用户ID
     * @return 待处理请求数量
     */
    long getPendingRequestCount(Long userId);

    /**
     * 批量处理好友请求
     * @param requestIds 请求ID列表
     * @param userId 用户ID
     * @param accept 是否接受（true为接受，false为拒绝）
     * @return 处理成功的数量
     */
    int batchProcessRequests(List<Long> requestIds, Long userId, boolean accept);

    /**
     * 删除已处理的请求记录
     * @param requestId 请求ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteProcessedRequest(Long requestId, Long userId);

    /**
     * 清理过期的好友请求
     * @param expireDays 过期天数
     * @return 清理的记录数
     */
    int cleanupExpiredRequests(int expireDays);

    /**
     * 获取最近的好友请求
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近的好友请求列表
     */
    List<ContactRequestResponse> getRecentRequests(Long userId, int limit);

    /**
     * 验证好友请求权限
     * @param requestId 请求ID
     * @param userId 用户ID
     * @return 是否有权限操作
     */
    boolean validateRequestPermission(Long requestId, Long userId);
}