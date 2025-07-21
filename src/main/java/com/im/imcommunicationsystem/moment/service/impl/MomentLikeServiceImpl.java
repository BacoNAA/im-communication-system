package com.im.imcommunicationsystem.moment.service.impl;

import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.entity.MomentLike;
import com.im.imcommunicationsystem.moment.exception.MomentNotFoundException;
import com.im.imcommunicationsystem.moment.repository.MomentLikeRepository;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import com.im.imcommunicationsystem.moment.service.MomentLikeService;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentLikeServiceImpl implements MomentLikeService {

    private final MomentRepository momentRepository;
    private final MomentLikeRepository likeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean likeMoment(Long momentId, Long userId) {
        log.info("用户 {} 点赞动态 {}", userId, momentId);
        
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 检查是否已经点赞
        Optional<MomentLike> existingLike = likeRepository.findByMomentIdAndUserId(momentId, userId);
        if (existingLike.isPresent()) {
            log.info("用户 {} 已经点赞过动态 {}", userId, momentId);
            // 已点赞，无需再次点赞
            return false;
        }
        
        // 创建点赞记录
        MomentLike like = new MomentLike();
        like.setMomentId(momentId);
        like.setUserId(userId);
        likeRepository.save(like);
        
        // 更新动态点赞数 - 处理likeCount可能为null的情况
        Integer currentLikes = moment.getLikeCount();
        if (currentLikes == null) {
            moment.setLikeCount(1); // 如果为null，设为1
        } else {
            moment.setLikeCount(currentLikes + 1);
        }
        momentRepository.save(moment);
        
        log.info("用户 {} 成功点赞动态 {}", userId, momentId);
        return true;
    }

    @Override
    @Transactional
    public boolean unlikeMoment(Long momentId, Long userId) {
        log.info("用户 {} 取消点赞动态 {}", userId, momentId);
        
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new MomentNotFoundException("动态不存在"));
        
        // 检查是否已经点赞
        Optional<MomentLike> existingLike = likeRepository.findByMomentIdAndUserId(momentId, userId);
        if (existingLike.isEmpty()) {
            log.info("用户 {} 未点赞过动态 {}, 无法取消", userId, momentId);
            // 未点赞，无法取消点赞
            return false;
        }
        
        // 删除点赞记录
        likeRepository.delete(existingLike.get());
        
        // 更新动态点赞数 - 处理likeCount可能为null的情况
        Integer currentLikes = moment.getLikeCount();
        if (currentLikes == null || currentLikes <= 0) {
            moment.setLikeCount(0); // 如果为null或已经是0，确保不会小于0
        } else {
            moment.setLikeCount(currentLikes - 1);
        }
        momentRepository.save(moment);
        
        log.info("用户 {} 成功取消点赞动态 {}", userId, momentId);
        return true;
    }

    @Override
    public boolean checkUserLiked(Long momentId, Long userId) {
        return likeRepository.findByMomentIdAndUserId(momentId, userId).isPresent();
    }

    @Override
    public long getLikeCount(Long momentId) {
        return likeRepository.countByMomentId(momentId);
    }

    @Override
    public Page<MomentDetailResponse.UserBriefInfo> getMomentLikes(Long momentId, Pageable pageable) {
        // 获取点赞记录
        Page<MomentLike> likes = likeRepository.findByMomentIdOrderByCreatedAtDesc(momentId, pageable);
        
        // 转换为用户简要信息
        return likes.map(like -> {
            MomentDetailResponse.UserBriefInfo userInfo = new MomentDetailResponse.UserBriefInfo();
            
            // 获取用户信息
            userRepository.findById(like.getUserId()).ifPresent(user -> {
                userInfo.setId(user.getId());
                userInfo.setNickname(user.getNickname());
                userInfo.setAvatar(user.getAvatarUrl());
            });
            
            return userInfo;
        });
    }

    @Override
    public List<Long> getUserLikedMomentIds(Long userId) {
        return likeRepository.findMomentIdsByUserId(userId);
    }
} 