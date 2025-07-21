package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.GroupSearchRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 群组搜索服务接口
 */
public interface GroupSearchService {
    
    /**
     * 搜索群组
     * 
     * @param request 搜索请求
     * @param userId 当前用户ID
     * @param pageable 分页参数
     * @return 群组搜索结果分页
     */
    Page<GroupSearchResponse> searchGroups(GroupSearchRequest request, Long userId, Pageable pageable);
    
    /**
     * 根据ID搜索群组
     * 
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @return 群组搜索结果，不存在则返回null
     */
    GroupSearchResponse getGroupById(Long groupId, Long userId);
} 