package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.AdminLoginRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminAuthResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.AdminUser;
import com.im.imcommunicationsystem.admin.enums.OperationType;
import com.im.imcommunicationsystem.admin.enums.TargetType;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.repository.AdminUserRepository;
import com.im.imcommunicationsystem.admin.service.AdminAuthService;
import com.im.imcommunicationsystem.auth.exception.AuthenticationException;
import com.im.imcommunicationsystem.auth.utils.PasswordUtils;
import com.im.imcommunicationsystem.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 管理员认证服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request) {
        log.info("开始处理管理员登录请求: {}", request.getUsername());

        try {
            // 1. 查找管理员用户
            Optional<AdminUser> adminUserOptional = adminUserRepository.findByUsername(request.getUsername());
            if (adminUserOptional.isEmpty()) {
                log.warn("管理员用户不存在: {}", request.getUsername());
                throw new AuthenticationException("用户名或密码错误");
            }

            AdminUser adminUser = adminUserOptional.get();
            log.debug("找到管理员用户: id={}, username={}, role={}, isActive={}", 
                    adminUser.getId(), adminUser.getUsername(), adminUser.getRole(), adminUser.getIsActive());

            // 2. 检查管理员账户是否激活
            if (!adminUser.getIsActive()) {
                log.warn("管理员账户已禁用: {}", request.getUsername());
                throw new AuthenticationException("账户已被禁用，请联系超级管理员");
            }

            // 3. 验证密码
            boolean passwordMatches = false;
            try {
                passwordMatches = passwordUtils.verifyPassword(request.getPassword(), adminUser.getPasswordHash());
                log.debug("密码验证结果: {}", passwordMatches);
            } catch (Exception e) {
                log.error("密码验证过程中发生异常: {}", e.getMessage(), e);
                throw new AuthenticationException("系统错误，请稍后重试");
            }
            
            if (!passwordMatches) {
                log.warn("管理员密码验证失败: {}", request.getUsername());
                throw new AuthenticationException("用户名或密码错误");
            }

            log.info("管理员密码验证成功: adminId={}, username={}", adminUser.getId(), adminUser.getUsername());

            // 4. 更新最后登录时间
            try {
                updateLastLoginTime(adminUser.getId());
            } catch (Exception e) {
                log.warn("更新管理员最后登录时间失败，但不影响登录流程: {}", e.getMessage());
                // 继续执行，不中断登录流程
            }

            // 5. 生成JWT令牌
            String accessToken;
            String refreshToken;
            try {
                accessToken = generateAccessToken(adminUser);
                refreshToken = generateRefreshToken(adminUser);
                log.debug("生成JWT令牌成功: accessToken={}, refreshToken={}", 
                        accessToken.substring(0, Math.min(10, accessToken.length())), 
                        refreshToken.substring(0, Math.min(10, refreshToken.length())));
            } catch (Exception e) {
                log.error("生成JWT令牌失败: {}", e.getMessage(), e);
                throw new AuthenticationException("生成认证令牌失败，请稍后重试");
            }

            // 6. 设置令牌过期时间
            Long expiresIn;
            if (Boolean.TRUE.equals(request.getRememberMe())) {
                expiresIn = 7 * 24 * 3600L; // 7天
                log.info("管理员选择记住登录状态，设置长期token: adminId={}", adminUser.getId());
            } else {
                expiresIn = 12 * 3600L; // 12小时
            }

            // 7. 记录登录操作日志
            try {
                log.debug("7. 开始记录管理员登录日志");
                
                AdminOperationLog operationLog = new AdminOperationLog();
                operationLog.setAdminId(adminUser.getId());
                operationLog.setTargetId(adminUser.getId());
                operationLog.setDescription("管理员登录系统");
                // 不记录IP地址
                operationLog.setIpAddress(null);
                operationLog.setCreatedAt(LocalDateTime.now());
                
                // 设置操作类型和目标类型字符串，直接使用字符串而非枚举
                operationLog.setOperationTypeString("LOGIN");
                operationLog.setTargetTypeString("ADMIN");
                
                log.debug("8. 准备保存登录操作日志");
                AdminOperationLog savedLog = adminOperationLogRepository.save(operationLog);
                log.debug("9. 保存管理员登录日志成功: logId={}", savedLog.getId());
            } catch (Exception e) {
                log.warn("保存管理员登录日志失败，但不影响登录流程: {}", e.getMessage(), e);
                // 继续执行，不中断登录流程
            }

            // 8. 构建响应
            AdminAuthResponse.AdminInfo adminInfo = AdminAuthResponse.AdminInfo.fromEntity(adminUser);
            log.info("管理员登录成功: adminId={}, username={}, role={}", 
                    adminUser.getId(), adminUser.getUsername(), adminUser.getRole());

            return AdminAuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .tokenType("Bearer")
                    .adminInfo(adminInfo)
                    .build();
        } catch (AuthenticationException e) {
            // 直接重新抛出认证异常
            throw e;
        } catch (Exception e) {
            // 记录详细错误日志并包装为认证异常
            log.error("管理员登录过程中发生未预期的异常: {}", e.getMessage(), e);
            throw new AuthenticationException("系统错误，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void logout(Long adminId) {
        log.info("管理员登出: adminId={}", adminId);

        try {
            // 记录登出操作日志
            AdminOperationLog operationLog = new AdminOperationLog();
            operationLog.setAdminId(adminId);
            operationLog.setTargetId(adminId);
            operationLog.setDescription("管理员登出系统");
            // 不记录IP地址
            operationLog.setIpAddress(null);
            operationLog.setCreatedAt(LocalDateTime.now());
            
            // 设置操作类型和目标类型字符串，直接使用字符串而非枚举
            operationLog.setOperationTypeString("LOGOUT");
            operationLog.setTargetTypeString("ADMIN");
            
            adminOperationLogRepository.save(operationLog);
            log.debug("保存管理员登出日志成功: logId={}", operationLog.getId());
        } catch (Exception e) {
            log.warn("保存管理员登出日志失败: {}", e.getMessage(), e);
        }

        // 注意：JWT令牌无法在服务端直接失效
        // 客户端需要删除本地存储的令牌
    }

    @Override
    public AdminAuthResponse.AdminInfo getAdminInfo(Long adminId) {
        log.info("获取管理员信息: adminId={}", adminId);

        try {
            AdminUser adminUser = adminUserRepository.findById(adminId)
                    .orElseThrow(() -> new AuthenticationException("管理员不存在"));
            
            return AdminAuthResponse.AdminInfo.fromEntity(adminUser);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取管理员信息失败: {}", e.getMessage(), e);
            throw new AuthenticationException("获取管理员信息失败");
        }
    }

    @Override
    public boolean validatePassword(Long adminId, String password) {
        log.info("验证管理员密码: adminId={}", adminId);

        try {
            AdminUser adminUser = adminUserRepository.findById(adminId)
                    .orElseThrow(() -> new AuthenticationException("管理员不存在"));

            return passwordUtils.verifyPassword(password, adminUser.getPasswordHash());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证管理员密码失败: {}", e.getMessage(), e);
            throw new AuthenticationException("验证密码失败");
        }
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Long adminId) {
        log.info("更新管理员最后登录时间: adminId={}", adminId);

        try {
            AdminUser adminUser = adminUserRepository.findById(adminId)
                    .orElseThrow(() -> new AuthenticationException("管理员不存在"));

            adminUser.setLastLoginAt(LocalDateTime.now());
            adminUserRepository.save(adminUser);
            log.debug("更新管理员最后登录时间成功");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新管理员最后登录时间失败: {}", e.getMessage(), e);
            throw new AuthenticationException("更新登录时间失败");
        }
    }

    /**
     * 生成管理员访问令牌
     */
    private String generateAccessToken(AdminUser adminUser) {
        // 确保角色名称带有ROLE_前缀，符合Spring Security的要求
        String roleName = adminUser.getRole().name().toUpperCase(); // 转换为大写
        String formattedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return jwtUtils.generateAccessToken(adminUser.getId(), adminUser.getUsername(), formattedRole);
    }

    /**
     * 生成管理员刷新令牌
     */
    private String generateRefreshToken(AdminUser adminUser) {
        return jwtUtils.generateRefreshToken(adminUser.getId(), adminUser.getUsername());
    }
    
    /**
     * 重置管理员密码
     * 
     * @param username 管理员用户名
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    @Override
    @Transactional
    public boolean resetPassword(String username, String newPassword) {
        log.info("开始重置管理员密码: username={}", username);
        
        try {
            log.debug("1. 查找管理员用户");
            // 查找管理员用户
            Optional<AdminUser> adminUserOptional = adminUserRepository.findByUsername(username);
            if (adminUserOptional.isEmpty()) {
                log.warn("管理员用户不存在: {}", username);
                return false;
            }
            
            AdminUser adminUser = adminUserOptional.get();
            log.debug("2. 已找到管理员用户: id={}, username={}", adminUser.getId(), adminUser.getUsername());
            
            try {
                log.debug("3. 加密新密码");
                // 加密新密码
                String newPasswordHash = passwordUtils.hashPassword(newPassword);
                log.debug("4. 密码加密成功");
                
                adminUser.setPasswordHash(newPasswordHash);
                
                log.debug("5. 保存更新到数据库");
                // 保存更新
                AdminUser savedUser = adminUserRepository.save(adminUser);
                log.debug("6. 保存成功: id={}", savedUser.getId());
                
                log.info("管理员密码重置成功: adminId={}, username={}", adminUser.getId(), adminUser.getUsername());
                
                // 记录操作日志
                try {
                    log.debug("7. 开始记录密码重置日志");
                    
                    AdminOperationLog operationLog = new AdminOperationLog();
                    operationLog.setAdminId(adminUser.getId());
                    operationLog.setTargetId(adminUser.getId());
                    operationLog.setDescription("管理员密码重置");
                    operationLog.setCreatedAt(LocalDateTime.now());
                    
                    // 设置操作类型和目标类型字符串，直接使用字符串而非枚举
                    operationLog.setOperationTypeString("RESET_PASSWORD");
                    operationLog.setTargetTypeString("ADMIN");
                    
                    // 对于密码重置，我们不记录IP地址
                    // operationLog.setIpAddress(null);
                    
                    log.debug("8. 准备保存操作日志");
                    AdminOperationLog savedLog = adminOperationLogRepository.save(operationLog);
                    log.debug("9. 保存密码重置日志成功: logId={}", savedLog.getId());
                } catch (Exception e) {
                    log.warn("记录管理员密码重置日志失败，但不影响密码重置: {}", e.getMessage(), e);
                    // 继续正常返回，不影响密码重置
                }
                
                return true;
            } catch (Exception e) {
                log.error("保存密码时发生异常: {}", e.getMessage(), e);
                throw new RuntimeException("保存密码失败", e);
            }
        } catch (RuntimeException e) {
            log.error("重置管理员密码过程中发生运行时异常: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("重置管理员密码过程中发生其他异常: {}", e.getMessage(), e);
            throw new RuntimeException("重置密码失败", e);
        }
    }
} 