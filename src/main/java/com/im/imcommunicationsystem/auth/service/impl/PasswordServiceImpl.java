package com.im.imcommunicationsystem.auth.service.impl;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.auth.service.PasswordService;
import com.im.imcommunicationsystem.auth.service.VerificationService;
import com.im.imcommunicationsystem.auth.utils.PasswordUtils;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 密码服务实现类
 * 处理密码相关业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final PasswordUtils passwordUtils;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ApiResponse<Void> changePassword(ChangePasswordRequest request) {
        log.info("开始处理修改密码请求");
        
        try {
            // 1. 验证新密码和确认密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new BusinessException("新密码和确认密码不一致");
            }
            
            // 2. 验证新密码强度
            if (!PasswordUtils.isStrongPassword(request.getNewPassword())) {
                throw new BusinessException("新密码不符合强度要求，密码长度必须在6-20位之间，且至少包含一个字母和一个数字");
            }
            
            // 3. 获取当前用户ID（从JWT token中获取）
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new BusinessException("用户未登录");
            }
            
            // 4. 查找用户
            Optional<User> userOptional = userRepository.findById(currentUserId);
            if (userOptional.isEmpty()) {
                throw new BusinessException("用户不存在");
            }
            
            User user = userOptional.get();
            
            // 5. 验证当前密码
            if (!passwordUtils.verifyPassword(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException("当前密码不正确");
            }
            
            // 6. 检查新密码是否与当前密码相同
            if (passwordUtils.verifyPassword(request.getNewPassword(), user.getPasswordHash())) {
                throw new BusinessException("新密码不能与当前密码相同");
            }
            
            // 7. 加密新密码并更新
            String newPasswordHash = passwordUtils.hashPassword(request.getNewPassword());
            user.setPasswordHash(newPasswordHash);
            userRepository.save(user);
            
            log.info("用户密码修改成功: userId={}", currentUserId);
            
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("密码修改成功")
                    .data(null)
                    .build();
                    
        } catch (BusinessException e) {
            log.warn("修改密码失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("修改密码时发生异常", e);
            throw new BusinessException("修改密码失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        log.info("开始处理重置密码请求: {}", request.getEmail());
        
        try {
            // 1. 验证新密码和确认密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new BusinessException("新密码和确认密码不一致");
            }
            
            // 2. 验证新密码强度
            if (!PasswordUtils.isStrongPassword(request.getNewPassword())) {
                throw new BusinessException("新密码不符合强度要求，密码长度必须在6-20位之间，且至少包含一个字母和一个数字");
            }
            
            // 3. 验证验证码
            boolean isValidCode = verificationService.verifyCode(
                request.getEmail(), 
                request.getVerificationCode(), 
                "reset_password"
            );
            
            if (!isValidCode) {
                throw new BusinessException("验证码错误或已过期");
            }
            
            // 4. 查找用户
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (userOptional.isEmpty()) {
                throw new BusinessException("该邮箱未注册");
            }
            
            User user = userOptional.get();
            
            // 5. 检查新密码是否与当前密码相同
            if (passwordUtils.verifyPassword(request.getNewPassword(), user.getPasswordHash())) {
                throw new BusinessException("新密码不能与当前密码相同");
            }
            
            // 6. 加密新密码并更新
            String newPasswordHash = passwordUtils.hashPassword(request.getNewPassword());
            user.setPasswordHash(newPasswordHash);
            userRepository.save(user);
            
            // 7. 删除已使用的验证码
            verificationService.deleteVerificationCode(request.getEmail(), "reset_password");
            
            log.info("用户密码重置成功: email={}", request.getEmail());
            
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("密码重置成功")
                    .data(null)
                    .build();
                    
        } catch (BusinessException e) {
            log.warn("重置密码失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("重置密码时发生异常", e);
            throw new BusinessException("重置密码失败，请稍后重试");
        }
    }

    @Override
    public boolean validateCurrentPassword(Long userId, String currentPassword) {
        log.info("验证当前密码: userId={}", userId);
        
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }
            
            User user = userOptional.get();
            boolean isValid = passwordUtils.verifyPassword(currentPassword, user.getPasswordHash());
            
            log.info("密码验证结果: userId={}, isValid={}", userId, isValid);
            return isValid;
            
        } catch (Exception e) {
            log.error("验证密码时发生异常: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public String encodePassword(String rawPassword) {
        log.info("加密密码");
        return passwordUtils.hashPassword(rawPassword);
    }
    
    /**
     * 获取当前用户ID
     * 从SecurityContext中获取当前用户ID
     * @return 当前用户ID
     */
    private Long getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }
}