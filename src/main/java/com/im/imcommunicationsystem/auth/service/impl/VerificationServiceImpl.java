package com.im.imcommunicationsystem.auth.service.impl;

import com.im.imcommunicationsystem.auth.entity.VerificationCode;
import com.im.imcommunicationsystem.auth.enums.VerificationCodeType;
import com.im.imcommunicationsystem.auth.exception.BusinessException;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.auth.repository.VerificationCodeRepository;
import com.im.imcommunicationsystem.auth.service.VerificationService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 验证服务实现类
 * 处理验证码相关业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public ApiResponse<Void> sendRegistrationCode(String email) {
        log.info("开始发送注册验证码到: {}", email);
        
        try {
            // 1. 检查邮箱是否已被注册
            if (userRepository.existsByEmail(email)) {
                log.warn("邮箱已被注册，拒绝发送注册验证码: {}", email);
                throw new BusinessException("该邮箱已被注册，请直接登录或使用其他邮箱");
            }
            
            // 2. 删除该邮箱之前的注册验证码
            verificationCodeRepository.deleteByEmailAndCodeType(email, VerificationCodeType.register);
            
            // 3. 生成新的验证码
            String code = generateCode();
            
            // 4. 保存验证码到数据库
            VerificationCode verificationCode = VerificationCode.builder()
                    .email(email)
                    .code(code)
                    .codeType(VerificationCodeType.register)
                    .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES))
                    .build();
            
            verificationCodeRepository.save(verificationCode);
            
            // 5. 发送验证码邮件
            try {
                emailService.sendVerificationCode(email, code, "【IM通信系统】邮箱注册验证码");
                log.info("注册验证码邮件发送成功: email={}, code={}, expiresAt={}", 
                        email, code, verificationCode.getExpiresAt());
            } catch (Exception emailException) {
                log.error("验证码邮件发送失败: email={}, error={}", email, emailException.getMessage(), emailException);
                // 删除已保存的验证码记录，因为邮件发送失败
                verificationCodeRepository.delete(verificationCode);
                throw new BusinessException("邮件发送失败，请检查邮箱地址是否正确或稍后重试");
            }
            
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("注册验证码发送成功")
                    .data(null)
                    .build();
                    
        } catch (Exception e) {
            log.error("发送注册验证码失败: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("发送验证码失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> sendLoginCode(String email) {
        log.info("开始发送登录验证码到: {}", email);
        
        try {
            // 1. 删除该邮箱之前的登录验证码
            verificationCodeRepository.deleteByEmailAndCodeType(email, VerificationCodeType.login);
            
            // 2. 生成新的验证码
            String code = generateCode();
            
            // 3. 保存验证码到数据库
            VerificationCode verificationCode = VerificationCode.builder()
                    .email(email)
                    .code(code)
                    .codeType(VerificationCodeType.login)
                    .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES))
                    .build();
            
            verificationCodeRepository.save(verificationCode);
            
            // 4. 发送验证码邮件
            try {
                emailService.sendVerificationCode(email, code, "【IM通信系统】登录验证码");
                log.info("登录验证码邮件发送成功: email={}, code={}", email, code);
            } catch (Exception emailException) {
                log.error("登录验证码邮件发送失败: email={}, error={}", email, emailException.getMessage(), emailException);
                verificationCodeRepository.delete(verificationCode);
                throw new BusinessException("邮件发送失败，请检查邮箱地址是否正确或稍后重试");
            }
            
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("登录验证码发送成功")
                    .data(null)
                    .build();
                    
        } catch (Exception e) {
            log.error("发送登录验证码失败: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("发送验证码失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> sendPasswordResetCode(String email) {
        log.info("开始发送密码重置验证码到: {}", email);
        
        try {
            // 1. 删除该邮箱之前的密码重置验证码
            verificationCodeRepository.deleteByEmailAndCodeType(email, VerificationCodeType.reset_password);
            
            // 2. 生成新的验证码
            String code = generateCode();
            
            // 3. 保存验证码到数据库（密码重置验证码有效期延长到10分钟）
            VerificationCode verificationCode = VerificationCode.builder()
                    .email(email)
                    .code(code)
                    .codeType(VerificationCodeType.reset_password)
                    .expiresAt(LocalDateTime.now().plusMinutes(10)) // 密码重置验证码有效期10分钟
                    .build();
            
            verificationCodeRepository.save(verificationCode);
            
            // 4. 发送验证码邮件
            try {
                emailService.sendVerificationCode(email, code, "【IM通信系统】密码重置验证码");
                log.info("密码重置验证码邮件发送成功: email={}, code={}", email, code);
            } catch (Exception emailException) {
                log.error("密码重置验证码邮件发送失败: email={}, error={}", email, emailException.getMessage(), emailException);
                verificationCodeRepository.delete(verificationCode);
                throw new BusinessException("邮件发送失败，请检查邮箱地址是否正确或稍后重试");
            }
            
            return ApiResponse.<Void>builder()
                    .code(200)
                    .message("密码重置验证码发送成功")
                    .data(null)
                    .build();
                    
        } catch (Exception e) {
            log.error("发送密码重置验证码失败: email={}, error={}", email, e.getMessage(), e);
            throw new BusinessException("发送验证码失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public boolean verifyCode(String email, String code, String codeType) {
        log.info("开始验证验证码: email={}, codeType={}", email, codeType);
        
        try {
            // 将字符串类型转换为枚举类型
            VerificationCodeType enumCodeType;
            try {
                enumCodeType = VerificationCodeType.valueOf(codeType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的验证码类型: {}", codeType);
                return false;
            }
            
            // 查找有效的验证码
            Optional<VerificationCode> optionalCode = verificationCodeRepository
                    .findByEmailAndCodeAndCodeTypeAndIsUsedFalseAndExpiresAtAfter(
                            email, code, enumCodeType, LocalDateTime.now());
            
            if (optionalCode.isEmpty()) {
                log.warn("验证码验证失败: email={}, code={}, codeType={} - 验证码无效或已过期", 
                        email, code, codeType);
                return false;
            }
            
            // 标记验证码为已使用
            VerificationCode verificationCode = optionalCode.get();
            verificationCode.setIsUsed(true);
            verificationCodeRepository.save(verificationCode);
            
            log.info("验证码验证成功: email={}, codeType={}", email, codeType);
            return true;
            
        } catch (Exception e) {
            log.error("验证码验证异常: email={}, codeType={}, error={}", 
                    email, codeType, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    @Override
    @Transactional
    public void cleanExpiredCodes() {
        try {
            verificationCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("清理过期验证码完成");
        } catch (Exception e) {
            log.error("清理过期验证码失败: {}", e.getMessage(), e);
        }
    }
}