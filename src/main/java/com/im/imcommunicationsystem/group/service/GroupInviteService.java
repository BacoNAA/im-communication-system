package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.JoinGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupInviteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 群邀请服务接口
 * 定义群邀请链接和二维码管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface GroupInviteService {

    /**
     * 生成群组邀请码
     * 
     * @param groupId 群组ID
     * @param inviterId 邀请者ID
     * @param maxUses 最大使用次数，null表示不限制
     * @param expiryMinutes 有效期（分钟），null表示不过期
     * @return 邀请响应
     */
    GroupInviteResponse generateInviteCode(Long groupId, Long inviterId, Integer maxUses, Integer expiryMinutes);

    /**
     * 通过邀请码加入群组
     * 
     * @param request 加入群组请求
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean joinGroupByInviteCode(JoinGroupRequest request, Long userId);

    /**
     * 获取群组邀请记录
     * 
     * @param groupId 群组ID
     * @param pageable 分页参数
     * @return 邀请分页列表
     */
    Page<GroupInviteResponse> getGroupInvites(Long groupId, Pageable pageable);

    /**
     * 禁用邀请码
     * 
     * @param inviteId 邀请ID
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean disableInvite(Long inviteId, Long operatorId);

    /**
     * 禁用群组所有邀请码
     * 
     * @param groupId 群组ID
     * @param operatorId 操作者ID
     * @return 禁用数量
     */
    int disableAllGroupInvites(Long groupId, Long operatorId);

    /**
     * 验证邀请码是否有效
     * 
     * @param inviteCode 邀请码
     * @return 是否有效
     */
    boolean validateInviteCode(String inviteCode);

    /**
     * 根据邀请码获取群组ID
     * 
     * @param inviteCode 邀请码
     * @return 群组ID，无效返回null
     */
    Long getGroupIdByInviteCode(String inviteCode);

    /**
     * 生成邀请二维码
     * 
     * @param inviteCode 邀请码
     * @return 二维码图片的Base64编码
     */
    String generateQRCode(String inviteCode);

    /**
     * 清理过期邀请
     * 
     * @return 清理数量
     */
    int cleanupExpiredInvites();
} 