package com.im.imcommunicationsystem.relationship.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.relationship.dto.request.SearchContactRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactSearchResponse;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import com.im.imcommunicationsystem.relationship.service.ContactSearchService;
import com.im.imcommunicationsystem.relationship.service.ContactTagAssignmentService;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagAssignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.im.imcommunicationsystem.relationship.dto.SetAliasRequest;
import com.im.imcommunicationsystem.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * 联系人管理控制器
 */
@Tag(name = "联系人管理", description = "联系人关系管理相关接口")
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;
    private final ContactSearchService contactSearchService;
    private final JwtUtils jwtUtils;
    private final ContactTagAssignmentService contactTagAssignmentService;

    /**
     * 获取联系人列表
     */
    @Operation(summary = "获取联系人列表", description = "获取用户的所有联系人列表")
    @GetMapping
    public ApiResponse<List<ContactResponse>> getContactList(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "是否包含被屏蔽的联系人") @RequestParam(defaultValue = "false") boolean includeBlocked) {
        try {
            log.info("获取联系人列表请求: userId={}, includeBlocked={}", userId, includeBlocked);
            List<ContactResponse> contacts = contactService.getContactList(userId, includeBlocked);
        
        // 添加详细的返回数据日志
        log.info("返回联系人列表数据: contacts.size()={}", contacts.size());
        for (ContactResponse contact : contacts) {
            log.info("返回联系人数据: userId={}, friendId={}, nickname={}, alias={}", 
                    contact.getUserId(), contact.getFriendId(), contact.getNickname(), contact.getAlias());
        }
        
        return ApiResponse.success(contacts);
        } catch (Exception e) {
            log.error("获取联系人列表失败: userId={}, includeBlocked={}, error={}", userId, includeBlocked, e.getMessage(), e);
            return ApiResponse.error(500, "获取联系人列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取联系人详情
     */
    @Operation(summary = "获取联系人详情", description = "获取指定联系人的详细信息")
    @GetMapping("/{friendId}")
    public ApiResponse<ContactResponse> getContactDetail(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID") @PathVariable Long friendId) {
        try {
            log.info("获取联系人详情请求: userId={}, friendId={}", userId, friendId);
            
            var contactDetail = contactService.getContactDetail(userId, friendId);
            if (contactDetail.isPresent()) {
                log.info("成功获取联系人详情: userId={}, friendId={}, tagCount={}", 
                        userId, friendId, contactDetail.get().getTagCount());
                return ApiResponse.success(contactDetail.get());
            } else {
                log.warn("联系人详情不存在: userId={}, friendId={}", userId, friendId);
                return ApiResponse.notFound("联系人不存在或已被删除");
            }
        } catch (Exception e) {
            log.error("获取联系人详情失败: userId={}, friendId={}, error={}", 
                    userId, friendId, e.getMessage(), e);
            return ApiResponse.error(500, "获取联系人详情失败: " + e.getMessage());
        }
    }

    /**
     * 设置好友备注
     */
    @Operation(summary = "设置好友备注", description = "为指定好友设置备注名")
    @PutMapping("/{friendId}/alias")
    public ApiResponse<Void> setContactAlias(
            @Parameter(description = "好友ID") @PathVariable Long friendId,
            @Parameter(description = "设置备注请求") @Valid @RequestBody SetAliasRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("设置好友备注请求: userId={}, friendId={}, alias={}", userId, friendId, request.getAlias());
            
            // 验证参数
            if (userId == null || friendId == null) {
                log.warn("设置好友备注失败: 参数无效");
                return ApiResponse.badRequest("用户ID和好友ID不能为空");
            }
            
            boolean success = contactService.setContactAlias(userId, friendId, request.getAlias());
            if (success) {
                log.info("成功设置好友备注: userId={}, friendId={}, alias={}", userId, friendId, request.getAlias());
                return ApiResponse.success("设置好友备注成功", null);
            } else {
                log.warn("设置好友备注失败: userId={}, friendId={}, alias={}", userId, friendId, request.getAlias());
                return ApiResponse.badRequest("设置好友备注失败，请检查好友关系是否存在");
            }
        } catch (Exception e) {
            log.error("设置好友备注异常: friendId={}, alias={}, error={}", 
                     friendId, request != null ? request.getAlias() : null, e.getMessage(), e);
            return ApiResponse.serverError("系统异常，请联系管理员");
        }
    }

    /**
     * 屏蔽联系人
     */
    @Operation(summary = "屏蔽联系人", description = "屏蔽指定的联系人")
    @PutMapping("/{friendId}/block")
    public ApiResponse<Void> blockContact(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID") @PathVariable Long friendId) {
        // 实现屏蔽联系人逻辑
        return null;
    }

    /**
     * 解除屏蔽
     */
    @Operation(summary = "解除屏蔽", description = "解除对指定联系人的屏蔽")
    @PutMapping("/{friendId}/unblock")
    public ApiResponse<Void> unblockContact(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID") @PathVariable Long friendId) {
        // 实现解除屏蔽逻辑
        return null;
    }

    /**
     * 删除好友关系
     */
    @Operation(summary = "删除好友", description = "删除与指定用户的好友关系")
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteContact(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID") @PathVariable Long friendId) {
        try {
            log.info("删除好友请求: userId={}, friendId={}", userId, friendId);
            boolean success = contactService.deleteContact(userId, friendId);
            if (success) {
                log.info("成功删除好友关系: userId={}, friendId={}", userId, friendId);
                return ApiResponse.success("删除好友成功", null);
            } else {
                 log.warn("删除好友失败: userId={}, friendId={}", userId, friendId);
                 return ApiResponse.badRequest("删除好友失败");
             }
         } catch (Exception e) {
             log.error("删除好友异常: userId={}, friendId={}, error={}", userId, friendId, e.getMessage(), e);
             return ApiResponse.serverError("系统异常，请联系管理员");
         }
    }

    /**
     * 搜索联系人
     */
    @Operation(summary = "搜索联系人", description = "根据关键词搜索联系人")
    @GetMapping("/search")
    public ApiResponse<List<ContactSearchResponse>> searchContacts(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "当前用户ID") @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        try {
            SearchContactRequest request = SearchContactRequest.builder()
                    .keyword(keyword)
                    .userId(userId)
                    .build();
            List<ContactSearchResponse> results = contactSearchService.searchUsers(request);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("搜索联系人失败: keyword={}, userId={}, error={}", keyword, userId, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取被屏蔽的联系人列表
     */
    @Operation(summary = "获取屏蔽列表", description = "获取用户屏蔽的所有联系人")
    @GetMapping("/blocked")
    public ApiResponse<List<ContactResponse>> getBlockedContacts(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        // 实现获取屏蔽列表逻辑
        return null;
    }

    /**
     * 获取好友数量
     */
    @Operation(summary = "获取好友数量", description = "获取用户的好友总数")
    @GetMapping("/count")
    public ApiResponse<Long> getFriendCount(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        // 实现获取好友数量逻辑
        return null;
    }

    /**
     * 批量删除联系人
     */
    @Operation(summary = "批量删除联系人", description = "批量删除多个联系人")
    @DeleteMapping("/batch")
    public ApiResponse<Integer> batchDeleteContacts(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID列表") @RequestBody List<Long> friendIds) {
        // 实现批量删除联系人逻辑
        return null;
    }

    /**
     * 批量屏蔽联系人
     */
    @Operation(summary = "批量屏蔽联系人", description = "批量屏蔽多个联系人")
    @PutMapping("/batch/block")
    public ApiResponse<Integer> batchBlockContacts(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "好友ID列表") @RequestBody List<Long> friendIds) {
        // 实现批量屏蔽联系人逻辑
        return null;
    }

    /**
     * 获取联系人的标签
     */
    @Operation(summary = "获取联系人标签", description = "获取指定联系人的所有标签")
    @GetMapping("/{friendId}/tags")
    public ApiResponse<List<ContactTagResponse>> getContactTags(
            @Parameter(description = "好友ID") @PathVariable Long friendId,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("获取联系人标签请求: userId={}, friendId={}", userId, friendId);
            List<ContactTagResponse> tags = contactTagAssignmentService.getContactTags(userId, friendId);
            return ApiResponse.success(tags);
        } catch (Exception e) {
            log.error("获取联系人标签失败: friendId={}, error={}", friendId, e.getMessage(), e);
            return ApiResponse.error(500, "获取联系人标签失败: " + e.getMessage());
        }
    }

    /**
     * 更新联系人的标签
     */
    @Operation(summary = "更新联系人标签", description = "替换指定联系人的所有标签")
    @PutMapping("/{friendId}/tags")
    public ApiResponse<Void> updateContactTags(
            @Parameter(description = "好友ID") @PathVariable Long friendId,
            @RequestBody Map<String, List<Long>> requestBody,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            List<Long> tagIds = requestBody.get("tagIds");
            log.info("更新联系人标签请求: userId={}, friendId={}, tagIds={}", userId, friendId, tagIds);
            
            contactTagAssignmentService.replaceContactTags(userId, friendId, tagIds);
            return ApiResponse.success("标签更新成功", null);
        } catch (Exception e) {
            log.error("更新联系人标签失败: friendId={}, error={}", friendId, e.getMessage(), e);
            return ApiResponse.error(500, "更新联系人标签失败: " + e.getMessage());
        }
    }
}