package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.response.GroupDetailsResponse;
import com.im.imcommunicationsystem.admin.dto.response.GroupMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 管理员群组管理服务接口
 */
public interface AdminGroupService {

    /**
     * 获取群组列表
     * @param pageable 分页参数
     * @param keyword 搜索关键词
     * @param status 状态过滤
     * @return 群组分页列表
     */
    Page<GroupDetailsResponse> getGroups(Pageable pageable, String keyword, String status);
    
    /**
     * 获取群组详情
     * @param groupId 群组ID
     * @return 群组详情
     */
    GroupDetailsResponse getGroupDetails(Long groupId);
    
    /**
     * 管理群组（封禁/解封等）
     * @param adminId 管理员ID
     * @param groupId 群组ID
     * @param action 操作类型
     * @param reason 操作原因
     * @param duration 封禁时长（小时）
     * @return 是否成功
     */
    boolean manageGroup(Long adminId, Long groupId, String action, String reason, Integer duration);
    
    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @param pageable 分页参数
     * @return 群组成员分页列表
     */
    Page<GroupMemberResponse> getGroupMembers(Long groupId, Pageable pageable);
} 