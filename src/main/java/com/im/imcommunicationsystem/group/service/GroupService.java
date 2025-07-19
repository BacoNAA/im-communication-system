package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.CreateGroupRequest;
import com.im.imcommunicationsystem.group.dto.request.UpdateGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupResponse;
import com.im.imcommunicationsystem.group.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 群组服务接口
 */
public interface GroupService {
    
    /**
     * 创建群组
     *
     * @param userId 创建者ID
     * @param request 创建群组请求
     * @return 创建的群组响应
     */
    GroupResponse createGroup(Long userId, CreateGroupRequest request);
    
    /**
     * 获取群组详情
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @return 群组响应
     */
    GroupResponse getGroupById(Long groupId, Long userId);
    
    /**
     * 获取用户的群组列表
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 群组分页列表
     */
    Page<GroupResponse> getUserGroups(Long userId, Pageable pageable);
    
    /**
     * 更新群组信息
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @param request 更新群组请求
     * @return 更新后的群组响应
     */
    GroupResponse updateGroup(Long groupId, Long userId, UpdateGroupRequest request);
    
    /**
     * 解散群组
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean dissolveGroup(Long groupId, Long userId);
    
    /**
     * 验证用户是否为群组成员
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为群组成员
     */
    boolean isGroupMember(Long groupId, Long userId);
    
    /**
     * 验证用户是否为群组管理员
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为群组管理员
     */
    boolean isGroupAdmin(Long groupId, Long userId);
    
    /**
     * 验证用户是否为群主
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否为群主
     */
    boolean isGroupOwner(Long groupId, Long userId);
    
    /**
     * 获取群组实体
     *
     * @param groupId 群组ID
     * @return 群组实体
     */
    Group getGroupEntityById(Long groupId);
} 