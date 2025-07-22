package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.response.GroupDetailsResponse;
import com.im.imcommunicationsystem.admin.service.AdminGroupService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员群组管理控制器
 */
@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
@Slf4j
public class AdminGroupController {

    private final AdminGroupService adminGroupService;
    private final SecurityUtils securityUtils;

    /**
     * 获取群组列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param keyword 搜索关键词（群组ID、名称）
     * @param status 群组状态筛选（all, active, banned）
     * @return 群组列表分页结果
     */
    @GetMapping
    public ApiResponse<Page<GroupDetailsResponse>> getGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status) {
        
        log.info("获取群组列表请求: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GroupDetailsResponse> groups = adminGroupService.getGroups(pageRequest, keyword, status);
        
        return ApiResponse.success("获取成功", groups);
    }

    /**
     * 获取群组详情
     * @param groupId 群组ID
     * @return 群组详细信息
     */
    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailsResponse> getGroupDetails(@PathVariable Long groupId) {
        log.info("获取群组详情请求: groupId={}", groupId);
        
        GroupDetailsResponse group = adminGroupService.getGroupDetails(groupId);
        return ApiResponse.success("获取成功", group);
    }

    /**
     * 管理群组（封禁/解封等）
     * @param groupId 群组ID
     * @param action 操作类型（ban, unban, dissolve）
     * @param reason 操作原因
     * @param duration 封禁时长（小时）
     * @return 操作结果
     */
    @PostMapping("/{groupId}/manage")
    public ApiResponse<String> manageGroup(
            @PathVariable Long groupId,
            @RequestParam String action,
            @RequestParam String reason,
            @RequestParam(required = false) Integer duration) {
        
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        log.info("管理群组请求: adminId={}, groupId={}, action={}, reason={}, duration={}", 
                adminId, groupId, action, reason, duration);
        
        boolean result = adminGroupService.manageGroup(adminId, groupId, action, reason, duration);
        
        if (result) {
            String message = switch(action.toLowerCase()) {
                case "ban" -> "群组已封禁";
                case "unban" -> "群组已解封";
                case "dissolve" -> "群组已解散";
                default -> "操作成功";
            };
            return ApiResponse.success(message, action);
        } else {
            return ApiResponse.error(500, "操作失败");
        }
    }

    /**
     * 获取群组成员
     * @param groupId 群组ID
     * @param page 页码
     * @param size 每页大小
     * @return 群组成员列表
     */
    @GetMapping("/{groupId}/members")
    public ApiResponse<?> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取群组成员请求: groupId={}, page={}, size={}", groupId, page, size);
        
        PageRequest pageRequest = PageRequest.of(page, size);
        var members = adminGroupService.getGroupMembers(groupId, pageRequest);
        
        return ApiResponse.success("获取成功", members);
    }
} 