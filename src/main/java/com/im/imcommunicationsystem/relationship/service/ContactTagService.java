package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.dto.request.ContactTagCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagUpdateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;

import java.util.List;

/**
 * 好友标签服务接口
 * 处理好友标签管理的业务逻辑
 */
public interface ContactTagService {

    /**
     * 创建好友标签
     * @param request 标签创建请求
     * @return 标签响应
     */
    ContactTagResponse createTag(ContactTagCreateRequest request);

    /**
     * 更新好友标签
     * @param tagId 标签ID
     * @param request 标签更新请求
     * @param userId 用户ID
     * @return 标签响应
     */
    ContactTagResponse updateTag(Long tagId, ContactTagUpdateRequest request, Long userId);

    /**
     * 删除好友标签
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteTag(Long tagId, Long userId);

    /**
     * 获取用户的所有标签
     * @param userId 用户ID
     * @return 标签响应列表
     */
    List<ContactTagResponse> getUserTags(Long userId);

    /**
     * 获取标签详情
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 标签响应
     */
    ContactTagResponse getTagDetail(Long tagId, Long userId);

    /**
     * 根据名称搜索标签
     * @param userId 用户ID
     * @param name 标签名称
     * @return 标签响应列表
     */
    List<ContactTagResponse> searchTagsByName(Long userId, String name);

    /**
     * 根据颜色查找标签
     * @param userId 用户ID
     * @param color 标签颜色
     * @return 标签响应列表
     */
    List<ContactTagResponse> getTagsByColor(Long userId, String color);

    /**
     * 检查标签名是否存在
     * @param userId 用户ID
     * @param name 标签名称
     * @return 是否存在
     */
    boolean isTagNameExists(Long userId, String name);

    /**
     * 获取标签数量
     * @param userId 用户ID
     * @return 标签数量
     */
    long getTagCount(Long userId);

    /**
     * 获取最近创建的标签
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近创建的标签列表
     */
    List<ContactTagResponse> getRecentTags(Long userId, int limit);

    /**
     * 批量删除标签
     * @param tagIds 标签ID列表
     * @param userId 用户ID
     * @return 删除成功的数量
     */
    int batchDeleteTags(List<Long> tagIds, Long userId);

    /**
     * 验证标签权限
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 是否有权限操作
     */
    boolean validateTagPermission(Long tagId, Long userId);

    /**
     * 复制标签
     * @param tagId 源标签ID
     * @param userId 用户ID
     * @param newName 新标签名称
     * @return 新标签响应
     */
    ContactTagResponse duplicateTag(Long tagId, Long userId, String newName);

    /**
     * 获取默认标签颜色列表
     * @return 颜色列表
     */
    List<String> getDefaultColors();
}