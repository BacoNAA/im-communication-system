package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.dto.request.SearchContactRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactSearchResponse;

import java.util.List;
import java.util.Optional;

/**
 * 联系人搜索服务接口
 */
public interface ContactSearchService {

    /**
     * 根据用户ID精确搜索用户
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    Optional<ContactSearchResponse> searchUserById(SearchContactRequest searchRequest);

    /**
     * 根据昵称模糊搜索用户
     * @param searchRequest 搜索请求
     * @return 搜索结果列表
     */
    List<ContactSearchResponse> searchUserByNickname(SearchContactRequest searchRequest);

    /**
     * 通用搜索方法
     * @param searchRequest 搜索请求
     * @return 搜索结果列表
     */
    List<ContactSearchResponse> searchUsers(SearchContactRequest searchRequest);

    /**
     * 根据二维码搜索用户
     * @param qrCodeContent 二维码内容
     * @param currentUserId 当前用户ID
     * @return 搜索结果
     */
    Optional<ContactSearchResponse> searchUserByQRCode(String qrCodeContent, Long currentUserId);

    /**
     * 验证用户是否允许被搜索
     * @param targetUserId 目标用户ID
     * @param currentUserId 当前用户ID
     * @return 是否允许搜索
     */
    boolean validateSearchPermission(Long targetUserId, Long currentUserId);

    /**
     * 检查用户关系状态
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 关系状态信息
     */
    String checkRelationshipStatus(Long currentUserId, Long targetUserId);
}