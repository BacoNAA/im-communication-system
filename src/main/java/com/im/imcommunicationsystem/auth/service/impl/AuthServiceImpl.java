package com.im.imcommunicationsystem.auth.service.impl;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.auth.dto.response.*;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.enums.VerificationCodeType;
import com.im.imcommunicationsystem.auth.exception.AuthenticationException;
import com.im.imcommunicationsystem.auth.exception.BusinessException;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.auth.service.AccountLockService;
import com.im.imcommunicationsystem.auth.service.AuthService;
import com.im.imcommunicationsystem.auth.service.DeviceService;
import com.im.imcommunicationsystem.auth.service.VerificationService;
import com.im.imcommunicationsystem.auth.utils.PasswordUtils;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证服务实现类
 * 处理用户认证相关业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final DeviceService deviceService;
    private final PasswordUtils passwordUtils;
    private final AccountLockService accountLockService;
    private final SecurityUtils securityUtils;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse registerByEmail(EmailRegistrationRequest request) {
        log.info("开始处理邮箱注册请求: {}", request.getEmail());
        
        // 1. 验证验证码
        boolean isCodeValid = verificationService.verifyCode(
            request.getEmail(), 
            request.getVerificationCode(), 
            VerificationCodeType.register.name()
        );
        if (!isCodeValid) {
            throw new AuthenticationException("验证码无效或已过期");
        }
        
        // 2. 检查邮箱是否已被注册
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }
        
        // 3. 验证昵称必填
        if (!StringUtils.hasText(request.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }
        
        // 4. 创建用户
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordUtils.hashPassword(request.getPassword()))
                .nickname(request.getNickname().trim())
                .build();
        
        user = userRepository.save(user);
        log.info("用户注册成功: userId={}, email={}", user.getId(), user.getEmail());
        
        // 5. 记录登录设备
        if (StringUtils.hasText(request.getDeviceType())) {
            deviceService.recordLoginDevice(user.getId(), request.getDeviceType(), request.getDeviceInfo(), null);
        }
        
        // 6. 生成JWT令牌
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        // 7. 构建用户信息响应
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(request.getPassword()) // 保存密码用于记住登录状态
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .userIdStr(user.getUserIdStr())
                .personalizedStatus(user.getStatusJson())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // 1小时
                .tokenType("Bearer")
                .requiresTwoFactor(false)
                .userInfo(userInfo)
                .build();
    }
    

    
    /**
     * 处理登录失败
     * @param email 用户邮箱
     * @return 不会返回，总是抛出异常
     */
    private AuthResponse handleLoginFailure(String email) {
        log.warn("处理登录失败: {}", email);
        
        try {
            // 检查账户是否已被锁定
            if (accountLockService.isAccountLocked(email)) {
                long remainingTime = accountLockService.getAccountLockRemainingTime(email);
                long remainingMinutes = Math.max(1, remainingTime / 60); // 至少显示1分钟
                long remainingSeconds = remainingTime % 60;
                
                String timeMessage;
                if (remainingMinutes > 0) {
                    timeMessage = String.format("%d分钟%d秒", remainingMinutes, remainingSeconds);
                } else {
                    timeMessage = String.format("%d秒", remainingSeconds);
                }
                
                log.warn("账户 {} 已被锁定，剩余时间: {}", email, timeMessage);
                throw new AuthenticationException("ACCOUNT_LOCKED", 
                    String.format("账户已被锁定，请在 %s 后重试。如需立即解锁，请联系管理员。", timeMessage));
            }
            
            // 记录登录失败并获取新的失败次数
            log.warn("记录登录失败: {}", email);
            int newAttempts = accountLockService.recordLoginFailure(email);
            log.warn("用户 {} 登录失败次数: {}", email, newAttempts);
            
            // 检查是否刚刚被锁定
            if (accountLockService.isAccountLocked(email)) {
                log.error("账户 {} 因连续失败次数过多被锁定", email);
                throw new AuthenticationException("ACCOUNT_LOCKED_BY_ATTEMPTS", 
                    "账号或密码错误。由于连续多次登录失败，账户已被锁定30分钟。如需立即解锁，请联系管理员。");
            }
            
            // 计算剩余尝试次数
            int remaining = Math.max(0, AccountLockService.MAX_LOGIN_ATTEMPTS - newAttempts);
            log.warn("用户 {} 剩余尝试次数: {}", email, remaining);
            
            if (remaining > 0) {
                throw new AuthenticationException("INVALID_CREDENTIALS", 
                    String.format("账号或密码错误。您还可以尝试 %d 次，超过限制后账户将被锁定30分钟。", remaining));
            } else {
                throw new AuthenticationException("ACCOUNT_LOCKED_BY_ATTEMPTS", 
                    "账号或密码错误。账户已被锁定30分钟，请稍后重试或联系管理员。");
            }
        } catch (AuthenticationException e) {
            // 重新抛出认证异常
            throw e;
        } catch (Exception e) {
            log.error("处理登录失败时发生系统异常: {}", email, e);
            throw new AuthenticationException("SYSTEM_ERROR", "登录验证时发生系统错误，请稍后重试或联系管理员。");
        }
    }
    
    /**
     * 生成访问令牌
     */
    private String generateAccessToken(User user) {
        return jwtUtils.generateAccessToken(user.getId(), user.getEmail(), "USER");
    }
    
    /**
     * 生成刷新令牌
     */
    private String generateRefreshToken(User user) {
        return jwtUtils.generateRefreshToken(user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public AuthResponse loginByPassword(PasswordLoginRequest request) {
        log.info("开始处理密码登录请求: {}", request.getEmail());
        
        // 1. 检查用户是否存在
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            log.warn("用户不存在: {}", request.getEmail());
            handleLoginFailure(request.getEmail());
            // 这里不会执行到，因为handleLoginFailure总是抛出异常
            throw new AuthenticationException("账号或密码错误");
        }
        
        User user = userOptional.get();
        
        // 2. 验证密码
        // 检查账户是否已被锁定
        if (accountLockService.isAccountLocked(user.getEmail())) {
            long remainingTime = accountLockService.getAccountLockRemainingTime(user.getEmail());
            long remainingMinutes = Math.max(1, remainingTime / 60);
            long remainingSeconds = remainingTime % 60;
            
            String timeMessage;
            if (remainingMinutes > 0) {
                timeMessage = String.format("%d分钟%d秒", remainingMinutes, remainingSeconds);
            } else {
                timeMessage = String.format("%d秒", remainingSeconds);
            }
            
            log.warn("用户 {} 尝试登录但账户已被锁定，剩余时间: {}", user.getEmail(), timeMessage);
            throw new AuthenticationException("ACCOUNT_LOCKED", 
                String.format("账户已被锁定，请在 %s 后重试。如需立即解锁，请联系管理员。", timeMessage));
        }

        if (!passwordUtils.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户 {} 密码验证失败", request.getEmail());
            handleLoginFailure(user.getEmail());
            // 这里不会执行到，因为handleLoginFailure总是抛出异常
            throw new AuthenticationException("INVALID_CREDENTIALS", "账号或密码错误");
        }
        
        // 3. 登录成功，清除失败记录
        accountLockService.clearLoginFailures(request.getEmail());
        log.info("用户密码验证成功: userId={}, email={}", user.getId(), user.getEmail());
        
        // 3. 记录登录设备
        if (StringUtils.hasText(request.getDeviceType())) {
            deviceService.recordLoginDevice(user.getId(), request.getDeviceType(), request.getDeviceInfo(), null);
        }
        
        // 4. 生成JWT令牌
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        // 5. 根据rememberMe设置token过期时间
        Long expiresIn;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            expiresIn = 30 * 24 * 3600L; // 30天
            log.info("用户选择记住登录状态，设置长期token: userId={}", user.getId());
        } else {
            expiresIn = 3600L; // 1小时
        }
        
        // 6. 构建用户信息响应
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(Boolean.TRUE.equals(request.getRememberMe()) ? request.getPassword() : null) // 只在记住登录状态时保存密码
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .userIdStr(user.getUserIdStr())
                .personalizedStatus(user.getStatusJson())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        
        log.info("用户登录成功: userId={}, email={}, rememberMe={}", user.getId(), user.getEmail(), request.getRememberMe());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .requiresTwoFactor(false)
                .userInfo(userInfo)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginByVerificationCode(VerificationCodeLoginRequest request) {
        log.info("开始处理验证码登录请求: {}", request.getEmail());
        
        // 1. 验证验证码
        boolean isCodeValid = verificationService.verifyCode(
            request.getEmail(), 
            request.getVerificationCode(), 
            VerificationCodeType.login.name()
        );
        if (!isCodeValid) {
            log.warn("验证码验证失败: {}", request.getEmail());
            throw new AuthenticationException("验证码错误或已失效");
        }
        
        // 2. 检查用户是否存在
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            log.warn("验证码登录时用户不存在: {}", request.getEmail());
            throw new AuthenticationException("该邮箱未注册");
        }
        
        User user = userOptional.get();
        log.info("验证码验证成功: userId={}, email={}", user.getId(), user.getEmail());
        
        // 3. 记录登录设备
        if (StringUtils.hasText(request.getDeviceType())) {
            deviceService.recordLoginDevice(user.getId(), request.getDeviceType(), request.getDeviceInfo(), null);
        }
        
        // 4. 生成JWT令牌
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        // 5. 构建用户信息响应
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .userIdStr(user.getUserIdStr())
                .personalizedStatus(user.getStatusJson())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        
        log.info("用户验证码登录成功: userId={}, email={}", user.getId(), user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // 1小时
                .tokenType("Bearer")
                .requiresTwoFactor(false)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public AuthResponse loginByAuthenticator(AuthenticatorLoginRequest request) {
        // TODO: 实现验证器登录逻辑
        log.info("验证器登录请求: {}", request.getTempToken());
        return AuthResponse.builder()
                .accessToken("temp_access_token")
                .refreshToken("temp_refresh_token")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .requiresTwoFactor(false)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.info("用户登出: userId={}, deviceType={}, clearRememberedInfo={}", 
                request.getUserId(), request.getDeviceType(), request.getClearRememberedInfo());
        
        // 1. 验证用户是否存在
        Optional<User> userOptional = userRepository.findById(request.getUserId());
        if (userOptional.isEmpty()) {
            log.warn("登出时用户不存在: userId={}", request.getUserId());
            throw new BusinessException("用户不存在");
        }
        
        User user = userOptional.get();
        
        // 2. 记录设备登出（如果提供了设备类型）
        if (StringUtils.hasText(request.getDeviceType())) {
            deviceService.logoutDevice(request.getUserId(), request.getDeviceType());
        }
        
        // 3. 这里可以添加其他登出逻辑，比如：
        // - 将当前token加入黑名单
        // - 清除服务器端的会话信息
        // - 记录登出日志等
        
        log.info("用户登出成功: userId={}, email={}", user.getId(), user.getEmail());
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // TODO: 实现刷新令牌逻辑
        log.info("刷新令牌请求: {}", refreshToken);
        return AuthResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .requiresTwoFactor(false)
                .build();
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        log.info("获取用户信息: userId={}", userId);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        
        User user = userOptional.get();
        
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                // 注意：getUserInfo方法不返回密码，出于安全考虑
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .userIdStr(user.getUserIdStr())
                .personalizedStatus(user.getStatusJson())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public boolean checkEmailExists(String email) {
        log.info("检查邮箱是否存在: {}", email);
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserInfoResponse getCurrentUserInfo() {
        log.info("获取当前用户信息");
        
        // 获取当前用户ID
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AuthenticationException("用户未登录或登录已过期");
        }
        
        // 获取用户信息
        return getUserInfo(currentUserId);
    }
}