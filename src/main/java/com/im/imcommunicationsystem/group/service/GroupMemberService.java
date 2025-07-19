package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.response.GroupMemberResponse;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 群成员服务接口
 */
public interface GroupMemberService {
    
    /**
     * 添加成员到群组
     *
     * @param groupId 群组ID
     * @param userIds 用户ID列表
     * @param operatorId 操作者ID
     * @return 添加的成员列表
     */
    List<GroupMemberResponse> addMembers(Long groupId, List<Long> userIds, Long operatorId);
    
    /**
     * 获取群成员列表
     *
     * @param groupId 群组ID
     * @param pageable 分页参数
     * @return 成员分页列表
     */
    Page<GroupMemberResponse> getGroupMembers(Long groupId, Pageable pageable);
    
    /**
     * 获取群管理员列表
     *
     * @param groupId 群组ID
     * @return 管理员列表
     */
    List<GroupMemberResponse> getGroupAdmins(Long groupId);
    
    /**
     * 移除群成员
     *
     * @param groupId 群组ID
     * @param userId 要移除的用户ID
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean removeMember(Long groupId, Long userId, Long operatorId);
    
    /**
     * 设置或取消管理员
     *
     * @param groupId 群组ID
     * @param userId 目标用户ID
     * @param isAdmin 是否设为管理员
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean setAdmin(Long groupId, Long userId, boolean isAdmin, Long operatorId);
    
    /**
     * 设置成员禁言状态
     *
     * @param groupId 群组ID
     * @param userId 目标用户ID
     * @param isMuted 是否禁言
     * @param minutes 禁言时间(分钟)，null表示永久
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean setMemberMuteStatus(Long groupId, Long userId, boolean isMuted, Integer minutes, Long operatorId);
    
    /**
     * 获取群成员实体
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 群成员实体
     */
    GroupMember getGroupMember(Long groupId, Long userId);
    
    /**
     * 统计群成员数量
     *
     * @param groupId 群组ID
     * @return 成员数量
     */
    int countGroupMembers(Long groupId);
} 