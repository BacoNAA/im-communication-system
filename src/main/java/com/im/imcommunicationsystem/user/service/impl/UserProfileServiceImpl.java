package com.im.imcommunicationsystem.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.exception.BusinessException;
import com.im.imcommunicationsystem.user.dto.request.SetStatusRequest;
import com.im.imcommunicationsystem.user.dto.request.SetUserIdRequest;
import com.im.imcommunicationsystem.user.dto.request.UpdateProfileRequest;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import com.im.imcommunicationsystem.user.exception.UserIdConflictException;
import com.im.imcommunicationsystem.user.exception.UserNotFoundException;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.user.service.FileUploadService;
import com.im.imcommunicationsystem.user.service.PublicFileUploadService;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import com.im.imcommunicationsystem.user.util.UserValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户资料服务实现类
 * 实现用户个人资料管理的核心业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final PublicFileUploadService publicFileUploadService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        log.info("获取用户资料，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + userId));
        
        return convertToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public void updateUserProfile(Long userId, UpdateProfileRequest request) {
        log.info("更新用户资料，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + userId));
        
        // 验证昵称
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            if (!UserValidationUtils.validateNickname(request.getNickname())) {
                throw new BusinessException("昵称格式不正确");
            }
            user.setNickname(request.getNickname().trim());
        }
        
        // 验证个性签名
        if (request.getSignature() != null) {
            if (request.getSignature().trim().isEmpty()) {
                // 允许设置为空
                user.setSignature(null);
            } else {
                if (!UserValidationUtils.validateSignature(request.getSignature())) {
                    throw new BusinessException("个性签名格式不正确");
                }
                user.setSignature(request.getSignature().trim());
            }
        } else {
            // 前端发送null时，也设置为null
            user.setSignature(null);
        }
        
        // 更新头像URL（如果提供）
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        
        // 更新手机号码
        if (request.getPhoneNumber() != null) {
            if (request.getPhoneNumber().trim().isEmpty()) {
                // 允许设置为空
                user.setPhoneNumber(null);
            } else {
                if (!UserValidationUtils.validatePhoneNumber(request.getPhoneNumber())) {
                    throw new BusinessException("手机号格式不正确");
                }
                user.setPhoneNumber(request.getPhoneNumber().trim());
            }
        } else {
            // 前端发送null时，也设置为null
            user.setPhoneNumber(null);
        }
        
        // 更新性别
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            if (!UserValidationUtils.validateGender(request.getGender())) {
                throw new BusinessException("性别格式不正确");
            }
            user.setGender(request.getGender().trim());
        }
        
        // 更新生日
        if (request.getBirthday() != null) {
            if (!UserValidationUtils.validateBirthday(request.getBirthday())) {
                throw new BusinessException("生日格式不正确");
            }
            user.setBirthday(request.getBirthday());
        }
        
        // 更新所在地
        if (request.getLocation() != null) {
            if (request.getLocation().trim().isEmpty()) {
                // 允许设置为空
                user.setLocation(null);
            } else {
                if (!UserValidationUtils.validateLocation(request.getLocation())) {
                    throw new BusinessException("所在地格式不正确");
                }
                user.setLocation(request.getLocation().trim());
            }
        } else {
            // 前端发送null时，也设置为null
            user.setLocation(null);
        }
        
        // 更新职业
        if (request.getOccupation() != null) {
            if (request.getOccupation().trim().isEmpty()) {
                // 允许设置为空
                user.setOccupation(null);
            } else {
                if (!UserValidationUtils.validateOccupation(request.getOccupation())) {
                    throw new BusinessException("职业格式不正确");
                }
                user.setOccupation(request.getOccupation().trim());
            }
        } else {
            // 前端发送null时，也设置为null
            user.setOccupation(null);
        }
        
        userRepository.save(user);
        log.info("用户资料更新成功，用户ID: {}", userId);
    }

    @Override
    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        log.info("开始更新用户头像: userId={}, fileName={}", userId, file.getOriginalFilename());
        
        try {
            // 验证用户是否存在
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 使用公开文件上传服务上传头像（头像属于公开文件）
            String avatarUrl = publicFileUploadService.uploadAvatar(file, userId);
            
            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            
            log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return avatarUrl;
            
        } catch (Exception e) {
            log.error("更新用户头像失败: userId={}", userId, e);
            throw new RuntimeException("更新头像失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void setPersonalId(Long userId, SetUserIdRequest request) {
        log.info("设置个人ID，用户ID: {}, 个人ID: {}", userId, request.getUserIdString());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + userId));
        
        String userIdStr = request.getUserIdString().trim();
        
        // 验证个人ID格式
        if (!UserValidationUtils.isValidUserId(userIdStr)) {
            throw new com.im.imcommunicationsystem.user.exception.UserIdValidationException(userIdStr);
        }
        
        // 检查修改频率限制（20秒间隔）
        if (user.getUserIdUpdatedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastUpdateTime = user.getUserIdUpdatedAt();
            long intervalSeconds = 20; // 20秒间隔
            LocalDateTime nextAllowedTime = lastUpdateTime.plusSeconds(intervalSeconds);
            
            if (now.isBefore(nextAllowedTime)) {
                throw new com.im.imcommunicationsystem.user.exception.UserIdUpdateTooFrequentException(
                    lastUpdateTime, nextAllowedTime, intervalSeconds);
            }
        }
        
        // 检查个人ID是否已被使用
        if (userRepository.existsByUserIdString(userIdStr)) {
            throw new UserIdConflictException("个人ID已被使用: " + userIdStr);
        }
        
        // 更新用户的个人ID和修改时间
        user.setUserIdStr(userIdStr);
        user.setUserIdUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("个人ID设置成功，用户ID: {}, 个人ID: {}", userId, userIdStr);
    }

    @Override
    public boolean validateUserIdStr(String userIdStr) {
        return UserValidationUtils.validateUserIdStr(userIdStr);
    }

    @Override
    @Transactional
    public void setUserStatus(Long userId, SetStatusRequest request) {
        log.info("设置用户状态，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + userId));
        
        // 验证状态文本
        if (!UserValidationUtils.validateStatusText(request.getStatusText())) {
            throw new BusinessException("状态文本格式不正确");
        }
        
        // 构建状态JSON
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("text", request.getStatusText());
        if (request.getEmoji() != null && !request.getEmoji().trim().isEmpty()) {
            statusMap.put("emoji", request.getEmoji().trim());
        }
        if (request.getExpiresAt() != null) {
            // 保存为ISO格式字符串，确保与前端格式一致
            statusMap.put("expiresAt", request.getExpiresAt().toString());
            log.info("设置状态过期时间: {}", request.getExpiresAt().toString());
        }
        statusMap.put("updatedAt", LocalDateTime.now().toString());
        
        try {
            String statusJson = objectMapper.writeValueAsString(statusMap);
            user.setStatusJson(statusJson);
            userRepository.save(user);
            log.info("用户状态设置成功，用户ID: {}", userId);
        } catch (JsonProcessingException e) {
            log.error("状态JSON序列化失败", e);
            throw new BusinessException("状态设置失败");
        }
    }

    @Override
    @Transactional
    public void clearUserStatus(Long userId) {
        log.info("清除用户状态，用户ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("用户不存在，ID: " + userId));
        
        user.setStatusJson(null);
        userRepository.save(user);
        
        log.info("用户状态清除成功，用户ID: {}", userId);
    }

    /**
     * 将User实体转换为UserProfileResponse
     */
    private UserProfileResponse convertToUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setSignature(user.getSignature());
        response.setUserIdString(user.getUserIdStr());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setLocation(user.getLocation());
        response.setOccupation(user.getOccupation());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // 解析状态JSON
        log.info("=== 调试信息 - 用户ID: {} ===", user.getId());
        log.info("=== 数据库中的statusJson: {} ===", user.getStatusJson());
        
        if (user.getStatusJson() != null && !user.getStatusJson().trim().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> statusMap = objectMapper.readValue(user.getStatusJson(), Map.class);
                log.info("=== 解析后的statusMap: {} ===", statusMap);
                log.info("=== statusMap的键: {} ===", statusMap.keySet());
                if (statusMap.containsKey("text")) {
                    log.info("=== 状态文本: {} ===", statusMap.get("text"));
                }
                if (statusMap.containsKey("emoji")) {
                    log.info("=== 状态表情: {} ===", statusMap.get("emoji"));
                }
                response.setStatus(statusMap);
            } catch (JsonProcessingException e) {
                log.warn("解析用户状态JSON失败，用户ID: {}", user.getId(), e);
                response.setStatus(null);
            }
        } else {
            log.info("=== 用户没有状态数据 ===");
            response.setStatus(null);
        }
        
        return response;
    }
}