package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.group.dto.request.PublishAnnouncementRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupAnnouncementResponse;
import com.im.imcommunicationsystem.group.service.GroupAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群公告控制器
 * 处理群公告相关请求
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupAnnouncementController {

    private final GroupAnnouncementService groupAnnouncementService;
    private final SecurityUtils securityUtils;

    /**
     * 发布群公告
     * 
     * @param groupId 群组ID
     * @param request 公告请求
     * @return 公告响应
     */
    @PostMapping("/{groupId}/announcements")
    public ResponseEntity<ApiResponse<GroupAnnouncementResponse>> publishAnnouncement(
            @PathVariable Long groupId,
            @Valid @RequestBody PublishAnnouncementRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]发布群组[{}]公告: {}", userId, groupId, request.getTitle());
        
        if (!groupAnnouncementService.validateAnnouncementPermission(groupId, userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权发布群公告"));
        }
        
        GroupAnnouncementResponse response = groupAnnouncementService.publishAnnouncement(groupId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新群公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @param request 公告请求
     * @return 公告响应
     */
    @PutMapping("/{groupId}/announcements/{announcementId}")
    public ResponseEntity<ApiResponse<GroupAnnouncementResponse>> updateAnnouncement(
            @PathVariable Long groupId,
            @PathVariable Long announcementId,
            @Valid @RequestBody PublishAnnouncementRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]更新群组[{}]公告[{}]: {}", userId, groupId, announcementId, request.getTitle());
        
        if (!groupAnnouncementService.validateAnnouncementPermission(groupId, userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权更新群公告"));
        }
        
        GroupAnnouncementResponse response = groupAnnouncementService.updateAnnouncement(
                groupId, announcementId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除群公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @return 成功状态
     */
    @DeleteMapping("/{groupId}/announcements/{announcementId}")
    public ResponseEntity<ApiResponse<String>> deleteAnnouncement(
            @PathVariable Long groupId,
            @PathVariable Long announcementId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]删除群组[{}]公告[{}]", userId, groupId, announcementId);
        
        if (!groupAnnouncementService.validateAnnouncementPermission(groupId, userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权删除群公告"));
        }
        
        boolean success = groupAnnouncementService.deleteAnnouncement(groupId, announcementId, userId);
        
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("公告已删除"));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "公告不存在或已被删除"));
        }
    }

    /**
     * 获取群公告列表
     * 
     * @param groupId 群组ID
     * @param pageable 分页参数
     * @return 公告分页列表
     */
    @GetMapping("/{groupId}/announcements")
    public ResponseEntity<ApiResponse<Page<GroupAnnouncementResponse>>> getAnnouncements(
            @PathVariable Long groupId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]获取群组[{}]公告列表", userId, groupId);
        Page<GroupAnnouncementResponse> announcements = groupAnnouncementService.getAnnouncements(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    /**
     * 获取置顶公告
     * 
     * @param groupId 群组ID
     * @return 置顶公告列表
     */
    @GetMapping("/{groupId}/announcements/pinned")
    public ResponseEntity<ApiResponse<List<GroupAnnouncementResponse>>> getPinnedAnnouncements(
            @PathVariable Long groupId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]获取群组[{}]置顶公告", userId, groupId);
        List<GroupAnnouncementResponse> announcements = groupAnnouncementService.getPinnedAnnouncements(groupId);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    /**
     * 置顶或取消置顶公告
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @param isPinned 是否置顶
     * @return 成功状态
     */
    @PutMapping("/{groupId}/announcements/{announcementId}/pin")
    public ResponseEntity<ApiResponse<String>> pinAnnouncement(
            @PathVariable Long groupId,
            @PathVariable Long announcementId,
            @RequestParam boolean isPinned) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]{}群组[{}]公告[{}]", userId, isPinned ? "置顶" : "取消置顶", groupId, announcementId);
        
        if (!groupAnnouncementService.validateAnnouncementPermission(groupId, userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作群公告"));
        }
        
        boolean success = groupAnnouncementService.pinAnnouncement(groupId, announcementId, isPinned, userId);
        
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(isPinned ? "公告已置顶" : "公告已取消置顶"));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "公告不存在或已被删除"));
        }
    }

    /**
     * 获取公告详情
     * 
     * @param groupId 群组ID
     * @param announcementId 公告ID
     * @return 公告响应
     */
    @GetMapping("/{groupId}/announcements/{announcementId}")
    public ResponseEntity<ApiResponse<GroupAnnouncementResponse>> getAnnouncementDetail(
            @PathVariable Long groupId,
            @PathVariable Long announcementId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
        }
        
        log.info("用户[{}]获取群组[{}]公告[{}]详情", userId, groupId, announcementId);
        GroupAnnouncementResponse announcement = groupAnnouncementService.getAnnouncementDetail(groupId, announcementId);
        
        if (announcement != null) {
            return ResponseEntity.ok(ApiResponse.success(announcement));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "公告不存在或已被删除"));
        }
    }
} 