package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.common.service.WebSocketService;
import com.im.imcommunicationsystem.group.dto.request.PublishAnnouncementRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupAnnouncementResponse;
import com.im.imcommunicationsystem.group.entity.GroupAnnouncement;
import com.im.imcommunicationsystem.group.repository.GroupAnnouncementRepository;
import com.im.imcommunicationsystem.group.service.GroupAnnouncementService;
import com.im.imcommunicationsystem.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 群公告服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupAnnouncementServiceImpl implements GroupAnnouncementService {

    private final GroupAnnouncementRepository announcementRepository;
    private final GroupService groupService;
    private final WebSocketService webSocketService;

    @Override
    @Transactional
    public GroupAnnouncementResponse publishAnnouncement(Long groupId, PublishAnnouncementRequest request, Long authorId) {
        // 检查群组是否存在
        com.im.imcommunicationsystem.group.entity.Group group = groupService.getGroupEntityById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群组不存在");
        }
        
        // 检查群组是否被封禁
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new IllegalArgumentException("该群组已被封禁，无法发布公告" + reason);
        }

        // 创建公告实体
        GroupAnnouncement announcement = GroupAnnouncement.builder()
                .groupId(groupId)
                .authorId(authorId)
                .title(request.getTitle())
                .content(request.getContent())
                .isPinned(request.getIsPinned() != null && request.getIsPinned())
                .build();

        // 如果要置顶该公告，则取消其他公告的置顶状态
        if (Boolean.TRUE.equals(announcement.getIsPinned())) {
            announcementRepository.findByGroupIdAndIsPinnedTrue(groupId)
                    .ifPresent(existingPinned -> {
                        existingPinned.setIsPinned(false);
                        announcementRepository.save(existingPinned);
                    });
        }

        // 保存公告
        announcement = announcementRepository.save(announcement);
        log.info("群组[{}]新增公告[{}]：{}", groupId, announcement.getId(), announcement.getTitle());

        // 转换为响应对象
        GroupAnnouncementResponse response = convertToResponse(announcement);
        
        // 发送WebSocket通知
        webSocketService.sendGroupUpdate(
            groupId, 
            response, 
            "ANNOUNCEMENT_NEW", 
            null
        );
        
        // 如果是置顶公告，发送置顶通知
        if (announcement.getIsPinned()) {
            webSocketService.sendGroupUpdate(
                groupId, 
                response, 
                "ANNOUNCEMENT_PINNED", 
                null
            );
        }

        // 返回响应
        return response;
    }

    @Override
    @Transactional
    public GroupAnnouncementResponse updateAnnouncement(Long groupId, Long announcementId, PublishAnnouncementRequest request, Long operatorId) {
        // 检查群组是否被封禁
        com.im.imcommunicationsystem.group.entity.Group group = groupService.getGroupEntityById(groupId);
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new IllegalArgumentException("该群组已被封禁，无法更新公告" + reason);
        }
        
        // 查找公告
        Optional<GroupAnnouncement> announcementOptional = announcementRepository.findById(announcementId);
        if (announcementOptional.isEmpty() || !announcementOptional.get().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("公告不存在或不属于该群组");
        }

        GroupAnnouncement announcement = announcementOptional.get();
        
        // 记录旧的置顶状态，用于判断是否发送置顶/取消置顶通知
        boolean wasOriginallyPinned = announcement.getIsPinned();

        // 更新公告信息
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());

        // 处理置顶状态
        boolean isPinned = request.getIsPinned() != null && request.getIsPinned();
        if (isPinned && !announcement.getIsPinned()) {
            // 如果要置顶该公告，则取消其他公告的置顶状态
            announcementRepository.findByGroupIdAndIsPinnedTrue(groupId)
                    .ifPresent(existingPinned -> {
                        if (!existingPinned.getId().equals(announcementId)) {
                            existingPinned.setIsPinned(false);
                            announcementRepository.save(existingPinned);
                        }
                    });
        }
        announcement.setIsPinned(isPinned);

        // 保存更新
        announcement = announcementRepository.save(announcement);
        log.info("群组[{}]的公告[{}]已更新", groupId, announcementId);

        // 转换为响应对象
        GroupAnnouncementResponse response = convertToResponse(announcement);
        
        // 发送WebSocket通知
        webSocketService.sendGroupUpdate(
            groupId, 
            response, 
            "ANNOUNCEMENT_UPDATE", 
            null
        );
        
        // 如果置顶状态发生变化，发送相应通知
        if (!wasOriginallyPinned && announcement.getIsPinned()) {
            webSocketService.sendGroupUpdate(
                groupId, 
                response, 
                "ANNOUNCEMENT_PINNED", 
                null
            );
        } else if (wasOriginallyPinned && !announcement.getIsPinned()) {
            webSocketService.sendGroupUpdate(
                groupId, 
                response, 
                "ANNOUNCEMENT_UNPINNED", 
                null
            );
        }

        // 返回响应
        return response;
    }

    @Override
    @Transactional
    public boolean deleteAnnouncement(Long groupId, Long announcementId, Long operatorId) {
        // 检查群组是否被封禁
        com.im.imcommunicationsystem.group.entity.Group group = groupService.getGroupEntityById(groupId);
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new IllegalArgumentException("该群组已被封禁，无法删除公告" + reason);
        }
        
        // 查找公告
        Optional<GroupAnnouncement> announcementOptional = announcementRepository.findById(announcementId);
        if (announcementOptional.isEmpty() || !announcementOptional.get().getGroupId().equals(groupId)) {
            return false;
        }
        
        // 转换为响应对象（在删除前）
        GroupAnnouncementResponse response = convertToResponse(announcementOptional.get());

        // 删除公告
        announcementRepository.deleteById(announcementId);
        log.info("群组[{}]的公告[{}]已删除", groupId, announcementId);
        
        // 发送WebSocket通知
        webSocketService.sendGroupUpdate(
            groupId, 
            response, 
            "ANNOUNCEMENT_DELETE", 
            null
        );
        
        return true;
    }

    @Override
    public Page<GroupAnnouncementResponse> getAnnouncements(Long groupId, Pageable pageable) {
        // 分页查询群公告
        Page<GroupAnnouncement> announcementPage = announcementRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        
        // 转换为响应对象
        return announcementPage.map(this::convertToResponse);
    }

    @Override
    public List<GroupAnnouncementResponse> getPinnedAnnouncements(Long groupId) {
        // 查询置顶的公告
        Optional<GroupAnnouncement> pinnedAnnouncement = announcementRepository.findByGroupIdAndIsPinnedTrue(groupId);
        
        return pinnedAnnouncement
                .map(announcement -> List.of(convertToResponse(announcement)))
                .orElse(List.of());
    }

    @Override
    @Transactional
    public boolean pinAnnouncement(Long groupId, Long announcementId, boolean isPinned, Long operatorId) {
        // 查找公告
        Optional<GroupAnnouncement> announcementOptional = announcementRepository.findById(announcementId);
        if (announcementOptional.isEmpty() || !announcementOptional.get().getGroupId().equals(groupId)) {
            return false;
        }

        GroupAnnouncement announcement = announcementOptional.get();
        
        // 如果状态已经是预期的，则不需要操作
        if (announcement.getIsPinned() == isPinned) {
            return true;
        }

        // 如果要置顶，先取消其他公告的置顶状态
        if (isPinned) {
            announcementRepository.findByGroupIdAndIsPinnedTrue(groupId)
                    .ifPresent(existingPinned -> {
                        if (!existingPinned.getId().equals(announcementId)) {
                            existingPinned.setIsPinned(false);
                            announcementRepository.save(existingPinned);
                        }
                    });
        }

        // 更新置顶状态
        announcement.setIsPinned(isPinned);
        announcementRepository.save(announcement);
        
        log.info("群组[{}]的公告[{}]置顶状态已更新为{}", groupId, announcementId, isPinned);
        
        // 转换为响应对象
        GroupAnnouncementResponse response = convertToResponse(announcement);
        
        // 发送WebSocket通知
        webSocketService.sendGroupUpdate(
            groupId, 
            response, 
            isPinned ? "ANNOUNCEMENT_PINNED" : "ANNOUNCEMENT_UNPINNED", 
            null
        );
        
        return true;
    }

    @Override
    public GroupAnnouncementResponse getAnnouncementDetail(Long groupId, Long announcementId) {
        // 查询公告详情
        return announcementRepository.findById(announcementId)
                .filter(announcement -> announcement.getGroupId().equals(groupId))
                .map(this::convertToResponse)
                .orElse(null);
    }

    @Override
    public boolean validateAnnouncementPermission(Long groupId, Long userId) {
        // 检查用户是否为群主或管理员
        return groupService.isGroupOwner(groupId, userId) || groupService.isGroupAdmin(groupId, userId);
    }

    /**
     * 将公告实体转换为响应对象
     */
    private GroupAnnouncementResponse convertToResponse(GroupAnnouncement announcement) {
        return GroupAnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .authorId(announcement.getAuthorId())
                .authorName("") // TODO: 根据authorId获取用户名
                .isPinned(announcement.getIsPinned())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
} 