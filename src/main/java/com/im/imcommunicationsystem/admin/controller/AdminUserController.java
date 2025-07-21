package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.UserManagementRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminUserResponse;
import com.im.imcommunicationsystem.admin.service.AdminUserService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 获取用户列表（分页）
     *
     * @param pageable 分页信息
     * @param keyword 搜索关键词
     * @param status 状态过滤
     * @return 包含用户列表的响应
     */
    @GetMapping
    public ResponseEntity<ResponseUtils.ApiResponse<Page<AdminUserResponse>>> getUserList(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        
        log.info("获取用户列表，关键词: {}, 状态: {}", keyword, status);
        Page<AdminUserResponse> users = adminUserService.getUserListWithPagination(pageable, keyword, status);
        
        return ResponseEntity.ok(ResponseUtils.success("用户列表获取成功", users));
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 包含用户详情的响应
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseUtils.ApiResponse<AdminUserResponse>> getUserDetails(@PathVariable Long userId) {
        log.info("获取用户详情，用户ID: {}", userId);
        AdminUserResponse user = adminUserService.getUserDetails(userId);
        
        return ResponseEntity.ok(ResponseUtils.success("用户详情获取成功", user));
    }

    /**
     * 管理用户（冻结/解冻）
     *
     * @param userId 用户ID
     * @param request 用户管理请求
     * @return 包含更新后用户的响应
     */
    @PostMapping("/{userId}/manage")
    public ResponseEntity<ResponseUtils.ApiResponse<AdminUserResponse>> manageUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserManagementRequest request) {
        
        log.info("管理用户，用户ID: {}，操作: {}", userId, request.getAction());
        
        // 从路径变量设置用户ID
        request.setUserId(userId);
        
        AdminUserResponse updatedUser = adminUserService.manageUser(request);
        
        return ResponseEntity.ok(ResponseUtils.success("用户管理操作成功", updatedUser));
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 包含成功消息的响应
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ResponseUtils.ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        log.info("删除用户，用户ID: {}", userId);
        adminUserService.deleteUser(userId);
        
        return ResponseEntity.ok(ResponseUtils.success("用户删除成功"));
    }
} 