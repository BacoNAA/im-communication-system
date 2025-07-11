package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.user.config.QRCodeConfig;
import com.im.imcommunicationsystem.user.dto.response.QRCodeResponse;
import com.im.imcommunicationsystem.user.service.UserQRCodeService;
import com.im.imcommunicationsystem.user.util.QRCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户二维码服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQRCodeServiceImpl implements UserQRCodeService {

    private final UserRepository userRepository;
    private final QRCodeConfig qrCodeConfig;

    @Override
    public QRCodeResponse generateUserQRCode(Long userId) {
        log.info("生成用户二维码: userId={}", userId);
        
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("用户不存在: userId={}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            User user = userOptional.get();
            
            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("nickname", user.getNickname());
            if (qrCodeConfig.getUserInfo().isIncludeAvatar() && user.getAvatarUrl() != null) {
                userInfo.put("avatarUrl", user.getAvatarUrl());
            }
            if (qrCodeConfig.getUserInfo().isIncludeSignature() && user.getSignature() != null) {
                userInfo.put("signature", user.getSignature());
            }
            userInfo.put("timestamp", System.currentTimeMillis());
            
            // 编码用户信息
            String qrCodeData = QRCodeUtils.encodeUserInfo(userInfo);
            
            // 生成二维码图片
            BufferedImage qrCodeImage = QRCodeUtils.generateQRCode(
                qrCodeData, 
                qrCodeConfig.getWidth(), 
                qrCodeConfig.getHeight()
            );
            
            // 转换为Base64
            String qrCodeBase64 = QRCodeUtils.imageToBase64(qrCodeImage, qrCodeConfig.getFormat());
            
            // 计算过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(qrCodeConfig.getExpirationHours());
            
            // 构建响应
            QRCodeResponse response = new QRCodeResponse();
            response.setQrCodeData(qrCodeData);
            response.setQrCodeBase64(qrCodeBase64);
            response.setUserId(user.getId());
            response.setUserIdString(user.getUserIdStr());
            response.setUserNickname(user.getNickname());
            response.setUserAvatarUrl(user.getAvatarUrl());
            response.setGeneratedAt(now);
            response.setExpiresAt(expiresAt);
            
            log.info("用户二维码生成成功: userId={}", userId);
            return response;
            
        } catch (Exception e) {
            log.error("生成用户二维码失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("生成二维码失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> parseUserQRCode(MultipartFile file) {
        log.info("解析用户二维码: fileName={}", file.getOriginalFilename());
        
        try {
            // 解析二维码内容
            String qrCodeContent = QRCodeUtils.parseQRCodeFile(file);
            
            if (qrCodeContent == null || qrCodeContent.trim().isEmpty()) {
                throw new RuntimeException("无法识别二维码内容");
            }
            
            // 解码用户信息
            Map<String, Object> userInfo = QRCodeUtils.decodeUserInfo(qrCodeContent);
            
            if (userInfo.isEmpty()) {
                throw new RuntimeException("二维码内容格式不正确");
            }
            
            // 验证用户是否存在
            Long userId = (Long) userInfo.get("userId");
            if (userId != null) {
                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    userInfo.put("userExists", true);
                    userInfo.put("currentNickname", user.getNickname());
                    userInfo.put("currentAvatarUrl", user.getAvatarUrl());
                } else {
                    userInfo.put("userExists", false);
                }
            }
            
            log.info("二维码解析完成: fileName={}, userId={}", file.getOriginalFilename(), userId);
            return userInfo;
        } catch (Exception e) {
            log.error("解析二维码失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("解析二维码失败: " + e.getMessage());
        }
    }

    @Override
    public String encodeUserInfo(Long userId) {
        log.info("编码用户信息: userId={}", userId);
        
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("用户不存在: userId={}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            User user = userOptional.get();
            
            // 构建用户信息Map
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("nickname", user.getNickname());
            userInfo.put("email", user.getEmail());
            
            if (qrCodeConfig.getUserInfo().isIncludeAvatar()) {
                userInfo.put("avatarUrl", user.getAvatarUrl());
            }
            
            if (qrCodeConfig.getUserInfo().isIncludeSignature()) {
                userInfo.put("signature", user.getSignature());
            }
            
            // 简单的编码实现（实际应该使用加密）
            String encoded = qrCodeConfig.getUserInfo().getPrefix() + userId + ":" + user.getNickname();
            
            log.info("用户信息编码完成: userId={}", userId);
            return encoded;
        } catch (Exception e) {
            log.error("编码用户信息失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("编码用户信息失败", e);
        }
    }

    @Override
    public Map<String, Object> decodeUserInfo(String qrCodeContent) {
        log.info("解码用户信息: content={}", qrCodeContent);
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 简单的解码实现
            if (qrCodeContent.startsWith(qrCodeConfig.getUserInfo().getPrefix())) {
                String content = qrCodeContent.substring(qrCodeConfig.getUserInfo().getPrefix().length());
                String[] parts = content.split(":", 2);
                
                if (parts.length >= 2) {
                    result.put("userId", Long.parseLong(parts[0]));
                    result.put("nickname", parts[1]);
                }
            }
            
            log.info("用户信息解码完成: content={}", qrCodeContent);
            return result;
        } catch (Exception e) {
            log.error("解码用户信息失败: content={}, error={}", qrCodeContent, e.getMessage(), e);
            throw new RuntimeException("解码用户信息失败", e);
        }
    }
}