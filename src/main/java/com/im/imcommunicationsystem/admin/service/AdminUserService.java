package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.UserManagementRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 管理员用户服务接口
 */
public interface AdminUserService {

    /**
     * 获取用户列表（分页）
     *
     * @param pageable 分页信息
     * @param keyword 搜索关键词（可选）
     * @param status 状态过滤（可选）
     * @return 用户响应对象分页结果
     */
    Page<AdminUserResponse> getUserListWithPagination(Pageable pageable, String keyword, String status);

    /**
     * 根据ID获取用户详情
     *
     * @param userId 用户ID
     * @return 用户响应对象
     */
    AdminUserResponse getUserDetails(Long userId);

    /**
     * 管理用户（冻结/解冻）
     *
     * @param request 用户管理请求
     * @return 更新后的用户响应对象
     */
    AdminUserResponse manageUser(UserManagementRequest request);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);
} 