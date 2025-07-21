package com.im.imcommunicationsystem.moment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest;
import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse;
import com.im.imcommunicationsystem.moment.dto.response.MomentResponse;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.entity.MomentLike;
import com.im.imcommunicationsystem.moment.enums.MediaType;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import com.im.imcommunicationsystem.moment.exception.MomentNotFoundException;
import com.im.imcommunicationsystem.moment.repository.MomentCommentRepository;
import com.im.imcommunicationsystem.moment.repository.MomentLikeRepository;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import com.im.imcommunicationsystem.moment.service.MomentService;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final MomentLikeRepository likeRepository;
    private final MomentCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MomentResponse createMoment(Long userId, CreateMomentRequest request) {
        // 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建动态实体
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(request.getContent());
        moment.setMediaType(request.getMediaType() != null ? request.getMediaType() : MediaType.TEXT);
        moment.setVisibilityType(request.getVisibilityType() != null ? request.getVisibilityType() : VisibilityType.PUBLIC);
        
        // 确保点赞和评论数初始化为0，避免空指针异常
        moment.setLikeCount(0);
        moment.setCommentCount(0);

        // 处理媒体文件URL
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            try {
                moment.setMediaUrls(objectMapper.writeValueAsString(request.getMediaUrls()));
            } catch (JsonProcessingException e) {
                log.error("处理媒体URL时出错", e);
                moment.setMediaUrls("[]");
            }
        } else {
            // 确保mediaUrls不为null
            moment.setMediaUrls("[]");
        }

        // 处理可见性规则
        if (request.getVisibilityType() == VisibilityType.CUSTOM && request.getVisibilityRules() != null) {
            try {
                moment.setVisibilityRules(objectMapper.writeValueAsString(request.getVisibilityRules()));
            } catch (JsonProcessingException e) {
                log.error("处理可见性规则时出错", e);
                moment.setVisibilityRules("{}");
            }
        } else {
            // 确保visibilityRules不为null
            moment.setVisibilityRules("{}");
        }

        // 保存动态前进行最后的检查确保必要字段已经设置
        if (moment.getLikeCount() == null) moment.setLikeCount(0);
        if (moment.getCommentCount() == null) moment.setCommentCount(0);

        // 保存动态
        Moment savedMoment = momentRepository.save(moment);

        // 转换为响应对象
        return convertToMomentResponse(savedMoment, userId);
    }

    @Override
    public Page<MomentResponse> getUserTimeline(Long userId, Pageable pageable) {
        // 获取用户自己的动态，包括私有动态
        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        log.info("获取用户{}的动态，包含私有动态，共{}条", userId, moments.getTotalElements());
        return moments.map(moment -> convertToMomentResponse(moment, userId));
    }

    @Override
    public Page<MomentResponse> getFriendTimeline(Long userId, Pageable pageable) {
        // 获取好友ID列表
        List<ContactResponse> contacts = contactService.getContactList(userId, false);
        List<Long> friendIds = contacts.stream()
                .map(ContactResponse::getFriendId)
                .collect(Collectors.toList());
        
        // 添加自己的ID，以便也能看到自己的动态
        friendIds.add(userId);
        
        // 获取好友的动态（包括私有动态）
        Page<Moment> moments = momentRepository.findFriendMoments(friendIds, pageable);
        log.info("获取到所有动态（包括私有动态），共{}条", moments.getTotalElements());
        
        // 过滤不可见的动态并转换为响应对象
        List<MomentResponse> visibleResponses = new ArrayList<>();
        for (Moment moment : moments.getContent()) {
            // 对于当前用户自己的动态，无论什么可见性都可见
            if (moment.getUserId().equals(userId)) {
                visibleResponses.add(convertToMomentResponse(moment, userId));
                continue;
            }
            
            // 对于其他人的动态，根据可见性规则过滤
            // PRIVATE类型动态只有自己可见，已在上面处理
            if (moment.getVisibilityType() == VisibilityType.PRIVATE) {
                continue;
            }
            
            // 其他类型动态需要检查可见性
            if (checkMomentVisibility(moment.getId(), userId)) {
                visibleResponses.add(convertToMomentResponse(moment, userId));
            }
        }
        
        log.info("过滤后可见的动态共{}条", visibleResponses.size());
        
        // 创建新的分页对象
        return new PageImpl<>(visibleResponses, pageable, moments.getTotalElements());
    }

    @Override
    public MomentDetailResponse getMomentDetail(Long momentId, Long userId) {
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 检查可见性
        if (!checkMomentVisibility(momentId, userId)) {
            throw new RuntimeException("无权查看此动态");
        }
        
        // 转换为详细响应对象
        MomentDetailResponse response = new MomentDetailResponse();
        
        // 复制基本信息
        MomentResponse baseResponse = convertToMomentResponse(moment, userId);
        response.setId(baseResponse.getId());
        response.setUserId(baseResponse.getUserId());
        response.setUserNickname(baseResponse.getUserNickname());
        response.setUserAvatar(baseResponse.getUserAvatar());
        response.setContent(baseResponse.getContent());
        response.setMediaUrls(baseResponse.getMediaUrls());
        response.setMediaType(baseResponse.getMediaType());
        response.setLikeCount(baseResponse.getLikeCount());
        response.setCommentCount(baseResponse.getCommentCount());
        response.setIsLiked(baseResponse.getIsLiked());
        response.setCreatedAt(baseResponse.getCreatedAt());
        
        // 设置可见性类型
        response.setVisibilityType(moment.getVisibilityType());
        
        // 获取点赞用户列表
        List<MomentDetailResponse.UserBriefInfo> likes = new ArrayList<>();
        likeRepository.findByMomentIdOrderByCreatedAtDesc(momentId, Pageable.ofSize(10))
                .forEach(like -> {
                    userRepository.findById(like.getUserId()).ifPresent(user -> {
                        likes.add(new MomentDetailResponse.UserBriefInfo(
                                user.getId(),
                                user.getNickname(),
                                user.getAvatarUrl()
                        ));
                    });
                });
        response.setLikes(likes);
        
        // TODO: 获取评论列表 - 需要评论服务支持
        response.setComments(Collections.emptyList());
        
        return response;
    }

    @Override
    @Transactional
    public MomentResponse updateMoment(Long momentId, Long userId, String content) {
        // 检查动态是否存在且属于当前用户
        Moment moment = momentRepository.findByIdAndUserId(momentId, userId)
                .orElseThrow(() -> new RuntimeException("动态不存在或无权修改"));
        
        // 更新内容
        moment.setContent(content);
        
        // 保存更新
        Moment updatedMoment = momentRepository.save(moment);
        
        // 转换为响应对象
        return convertToMomentResponse(updatedMoment, userId);
    }

    @Override
    @Transactional
    public boolean deleteMoment(Long momentId, Long userId) {
        // 检查动态是否存在且属于当前用户
        Moment moment = momentRepository.findByIdAndUserId(momentId, userId)
                .orElseThrow(() -> new RuntimeException("动态不存在或无权删除"));
        
        // 删除动态
        momentRepository.delete(moment);
        
        return true;
    }

    @Override
    public boolean checkMomentVisibility(Long momentId, Long userId) {
        // 获取动态
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 动态作者可以查看自己的动态
        if (moment.getUserId().equals(userId)) {
            log.info("作者 {} 可以查看自己的动态 {}", userId, momentId);
            return true;
        }
        
        // 首先检查是否为好友，非好友关系不能查看任何类型的动态
        if (!contactService.isFriend(moment.getUserId(), userId)) {
            log.info("用户 {} 不是作者 {} 的好友，不能查看动态 {}", userId, moment.getUserId(), momentId);
            return false;
        }
        
        // 根据可见性类型判断
        switch (moment.getVisibilityType()) {
            case PUBLIC:
                // 公开动态：所有好友都可见（我们已经验证了好友关系）
                log.info("动态 {} 是公开类型，好友 {} 可以查看", momentId, userId);
                return true;
                
            case PRIVATE:
                // 私密动态仅作者可见
                log.info("动态 {} 是私密类型，好友 {} 不能查看", momentId, userId);
                return false;
                
            case CUSTOM:
                // 自定义可见性需要解析规则
                try {
                    if (moment.getVisibilityRules() == null) {
                        log.info("动态 {} 没有自定义规则，默认好友 {} 不可见", momentId, userId);
                        return false;
                    }
                    
                    CreateMomentRequest.VisibilityRules rules = objectMapper.readValue(
                            moment.getVisibilityRules(),
                            CreateMomentRequest.VisibilityRules.class
                    );
                    
                    // 检查是否在允许列表中
                    if (rules.getAllowedUserIds() != null && !rules.getAllowedUserIds().isEmpty()) {
                        boolean allowed = rules.getAllowedUserIds().contains(userId);
                        log.info("动态 {} 有允许名单，用户 {} {}", momentId, userId, allowed ? "在名单中" : "不在名单中");
                        return allowed;
                    }
                    
                    // 检查是否在阻止列表中
                    if (rules.getBlockedUserIds() != null && !rules.getBlockedUserIds().isEmpty()) {
                        boolean blocked = rules.getBlockedUserIds().contains(userId);
                        log.info("动态 {} 有屏蔽名单，用户 {} {}", momentId, userId, blocked ? "在屏蔽名单中" : "不在屏蔽名单中");
                        return !blocked; // 不在阻止列表中就可见
                    }
                    
                    // 默认规则：好友关系可见（已经在前面验证了好友关系）
                    log.info("动态 {} 使用默认规则，好友 {} 可见", momentId, userId);
                    return true;
                    
                } catch (Exception e) {
                    log.error("解析动态 {} 的可见性规则时出错: {}", momentId, e.getMessage(), e);
                    return false;
                }
                
            default:
                log.warn("动态 {} 使用了未知的可见性类型 {}", momentId, moment.getVisibilityType());
                return false;
        }
    }
    
    /**
     * 辅助方法：将动态实体转换为响应对象
     */
    private MomentResponse convertToMomentResponse(Moment moment, Long currentUserId) {
        MomentResponse response = MomentResponse.builder()
                .id(moment.getId())
                .userId(moment.getUserId())
                .content(moment.getContent())
                .mediaType(moment.getMediaType())
                .likeCount(moment.getLikeCount() != null ? moment.getLikeCount() : 0)
                .commentCount(moment.getCommentCount() != null ? moment.getCommentCount() : 0)
                .createdAt(moment.getCreatedAt())
                .build();
        
        // 获取用户信息
        userRepository.findById(moment.getUserId()).ifPresent(user -> {
            response.setUserNickname(user.getNickname());
            response.setUserAvatar(user.getAvatarUrl());
        });
        
        // 解析媒体URL列表
        if (moment.getMediaUrls() != null && !moment.getMediaUrls().isEmpty()) {
            try {
                List<String> mediaUrls = objectMapper.readValue(moment.getMediaUrls(), List.class);
                response.setMediaUrls(mediaUrls);
            } catch (Exception e) {
                log.error("解析媒体URL列表时出错", e);
                response.setMediaUrls(Collections.emptyList());
            }
        } else {
            response.setMediaUrls(Collections.emptyList());
        }
        
        // 检查当前用户是否点赞
        Optional<MomentLike> like = likeRepository.findByMomentIdAndUserId(moment.getId(), currentUserId);
        response.setIsLiked(like.isPresent());
        
        return response;
    }
} 