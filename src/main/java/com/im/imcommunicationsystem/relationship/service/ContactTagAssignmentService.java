package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.dto.request.ContactTagAssignRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagAssignmentResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;

import java.util.List;

/**
 * 联系人标签分配服务接口
 * 负责管理联系人与标签的多对多关系
 */
public interface ContactTagAssignmentService {

    /**
     * 为联系人分配标签
     * @param request 分配请求
     * @return 分配结果列表
     */
    List<ContactTagAssignmentResponse> assignTagsToContact(ContactTagAssignRequest request);

    /**
     * 移除联系人的标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagIds 要移除的标签ID列表
     * @return 移除的数量
     */
    int removeTagsFromContact(Long userId, Long friendId, List<Long> tagIds);

    /**
     * 获取联系人的所有标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 标签列表
     */
    List<ContactTagResponse> getContactTags(Long userId, Long friendId);

    /**
     * 获取标签下的所有联系人
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 联系人ID列表
     */
    List<Long> getContactsByTag(Long userId, Long tagId);

    /**
     * 批量获取多个联系人的标签
     * @param userId 用户ID
     * @param friendIds 好友ID列表
     * @return 联系人标签映射
     */
    java.util.Map<Long, List<ContactTagResponse>> getBatchContactTags(Long userId, List<Long> friendIds);

    /**
     * 替换联系人的所有标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagIds 新的标签ID列表
     * @return 分配结果列表
     */
    List<ContactTagAssignmentResponse> replaceContactTags(Long userId, Long friendId, List<Long> tagIds);

    /**
     * 检查联系人是否有指定标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagId 标签ID
     * @return 是否有该标签
     */
    boolean hasTag(Long userId, Long friendId, Long tagId);

    /**
     * 获取联系人的标签数量
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 标签数量
     */
    int getContactTagCount(Long userId, Long friendId);

    /**
     * 清除联系人的所有标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 清除的数量
     */
    int clearContactTags(Long userId, Long friendId);

    /**
     * 获取使用指定标签的联系人数量
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 联系人数量
     */
    long getTagUsageCount(Long userId, Long tagId);

    /**
     * 验证标签分配权限
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagId 标签ID
     * @return 是否有权限
     */
    boolean validateAssignmentPermission(Long userId, Long friendId, Long tagId);

    /**
     * 获取标签下的所有联系人详细信息
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 联系人详细信息列表
     */
    List<com.im.imcommunicationsystem.relationship.dto.response.ContactResponse> getContactDetailsByTag(Long userId, Long tagId);
}