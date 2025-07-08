package com.im.imcommunicationsystem.controller;

import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import com.im.imcommunicationsystem.auth.service.AccountLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 账号锁定管理控制器
 * 提供账号锁定状态查询、手动解锁、清除登录失败记录、手动锁定等功能
 */
@RestController
@RequestMapping("/api/account-lock")
public class AccountLockManagementController {

    @Autowired
    private AccountLockService accountLockService;

    /**
     * 查询账号锁定状态
     * @param email 用户邮箱
     * @return 锁定状态信息
     */
    @GetMapping("/status/{email}")
    public ResponseUtils.ApiResponse<?> getAccountLockStatus(@PathVariable String email) {
        try {
            boolean isLocked = accountLockService.isAccountLocked(email);
            int failedAttempts = accountLockService.getLoginAttempts(email);
            
            final String userEmail = email;
            final boolean accountLocked = isLocked;
            final int attempts = failedAttempts;
            
            return ResponseUtils.success("查询成功", new Object() {
                public final String email = userEmail;
                public final boolean isLocked = accountLocked;
                public final int failedAttempts = attempts;
                public final String status = accountLocked ? "已锁定" : "正常";
            });
        } catch (Exception e) {
            return ResponseUtils.error("查询账号锁定状态失败: " + e.getMessage());
        }
    }

    /**
     * 手动解锁账号
     * @param email 用户邮箱
     * @return 解锁结果
     */
    @PostMapping("/unlock")
    public ResponseUtils.ApiResponse<?> unlockAccount(@RequestBody UnlockRequest request) {
        try {
            accountLockService.unlockAccount(request.getEmail());
            return ResponseUtils.success("账号解锁成功");
        } catch (Exception e) {
            return ResponseUtils.error("解锁账号失败: " + e.getMessage());
        }
    }

    /**
     * 清除登录失败记录
     * @param email 用户邮箱
     * @return 清除结果
     */
    @PostMapping("/clear-failed-attempts")
    public ResponseUtils.ApiResponse<?> clearFailedAttempts(@RequestBody ClearFailedAttemptsRequest request) {
        try {
            accountLockService.clearLoginFailures(request.getEmail());
            return ResponseUtils.success("登录失败记录清除成功");
        } catch (Exception e) {
            return ResponseUtils.error("清除登录失败记录失败: " + e.getMessage());
        }
    }

    /**
     * 手动锁定账号
     * @param request 锁定请求
     * @return 锁定结果
     */
    @PostMapping("/lock")
    public ResponseUtils.ApiResponse<?> lockAccount(@RequestBody LockAccountRequest request) {
        try {
            accountLockService.lockAccount(request.getEmail());
            return ResponseUtils.success("账号锁定成功");
        } catch (Exception e) {
            return ResponseUtils.error("锁定账号失败: " + e.getMessage());
        }
    }

    // 请求DTO类
    public static class UnlockRequest {
        private String email;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ClearFailedAttemptsRequest {
        private String email;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class LockAccountRequest {
        private String email;
        private String reason;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}