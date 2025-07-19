package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.PublishAnnouncementRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupAnnouncementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 群公告服务接口
 * 定义群公告相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface GroupAnnouncementService {

    /**
     * 发布群公告
     * 
     * @param groupId 群组ID
     * @param request 公告请求
     * @param authorId 作者ID
     * @return 公告响应
     */
    GroupAnnouncementResponse publishAnnouncement(Long groupId, PublishAnnouncementRequest request, Long authorId);

    /**
     * 更新群公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @param request 公告请求
     * @param operatorId 操作者ID
     * @return 公告响应
     */
    GroupAnnouncementResponse updateAnnouncement(Long groupId, Long announcementId, PublishAnnouncementRequest request, Long operatorId);

    /**
     * 删除群公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean deleteAnnouncement(Long groupId, Long announcementId, Long operatorId);

    /**
     * 获取群公告列表
     * 
     * @param groupId 群组ID
     * @param pageable 分页参数
     * @return 公告分页列表
     */
    Page<GroupAnnouncementResponse> getAnnouncements(Long groupId, Pageable pageable);

    /**
     * 获取置顶公告
     * 
     * @param groupId 群组ID
     * @return 置顶公告列表
     */
    List<GroupAnnouncementResponse> getPinnedAnnouncements(Long groupId);

    /**
     * 置顶或取消置顶公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @param isPinned 是否置顶
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean pinAnnouncement(Long groupId, Long announcementId, boolean isPinned, Long operatorId);

    /**
     * 获取公告详情
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @return 公告响应
     */
    GroupAnnouncementResponse getAnnouncementDetail(Long groupId, Long announcementId);

    /**
     * 验证用户是否有权限管理公告
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean validateAnnouncementPermission(Long groupId, Long userId);
} 