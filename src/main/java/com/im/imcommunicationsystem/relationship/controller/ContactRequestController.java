package com.im.imcommunicationsystem.relationship.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.relationship.dto.request.ContactRequestCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactRequestResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactRequestStatsResponse;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import com.im.imcommunicationsystem.relationship.service.ContactRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友请求管理控制器
 */
@Tag(name = "好友请求管理", description = "好友请求相关接口")
@RestController
@RequestMapping("/api/contact-requests")
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestService contactRequestService;
    private final SecurityUtils securityUtils;

    /**
     * 发送好友请求
     */
    @Operation(summary = "发送好友请求", description = "向指定用户发送好友请求")
    @PostMapping
    public ApiResponse<Void> sendContactRequest(@Valid @RequestBody ContactRequestCreateRequest request) {
        try {
            boolean success = contactRequestService.sendContactRequest(request);
            if (success) {
                return ApiResponse.success("好友请求发送成功", null);
            } else {
                return ApiResponse.badRequest("好友请求发送失败");
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("发送好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 接受好友请求
     */
    @Operation(summary = "接受好友请求", description = "接受指定的好友请求")
    @PutMapping("/{requestId}/accept")
    public ApiResponse<Void> acceptContactRequest(
            @Parameter(description = "请求ID") @PathVariable Long requestId,
            @Parameter(description = "接收者ID") @RequestParam Long recipientId) {
        try {
            boolean success = contactRequestService.acceptContactRequest(requestId, recipientId);
            if (success) {
                return ApiResponse.success("好友请求接受成功", null);
            } else {
                return ApiResponse.badRequest("好友请求接受失败");
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("接受好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝好友请求
     */
    @Operation(summary = "拒绝好友请求", description = "拒绝指定的好友请求")
    @PutMapping("/{requestId}/reject")
    public ApiResponse<Void> rejectContactRequest(
            @Parameter(description = "请求ID") @PathVariable Long requestId,
            @Parameter(description = "接收者ID") @RequestParam Long recipientId) {
        try {
            boolean success = contactRequestService.rejectContactRequest(requestId, recipientId);
            if (success) {
                return ApiResponse.success("好友请求拒绝成功", null);
            } else {
                return ApiResponse.badRequest("好友请求拒绝失败");
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("拒绝好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 撤回好友请求
     */
    @Operation(summary = "撤回好友请求", description = "撤回已发送的好友请求")
    @DeleteMapping("/{requestId}")
    public ApiResponse<Void> withdrawContactRequest(
            @Parameter(description = "请求ID") @PathVariable Long requestId,
            @Parameter(description = "请求者ID") @RequestParam Long requesterId) {
        try {
            boolean success = contactRequestService.withdrawContactRequest(requestId, requesterId);
            if (success) {
                return ApiResponse.success("好友请求撤回成功", null);
            } else {
                return ApiResponse.badRequest("好友请求撤回失败");
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("撤回好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取收到的好友请求列表
     */
    @Operation(summary = "获取收到的好友请求", description = "获取用户收到的好友请求列表")
    @GetMapping("/received")
    public ApiResponse<List<ContactRequestResponse>> getReceivedRequests(
            @Parameter(description = "请求状态") @RequestParam(required = false) ContactRequestStatus status) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<ContactRequestResponse> requests = contactRequestService.getReceivedRequests(userId, status);
            return ApiResponse.success("获取收到的好友请求成功", requests);
        } catch (Exception e) {
            return ApiResponse.badRequest("获取收到的好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取发送的好友请求列表
     */
    @Operation(summary = "获取发送的好友请求", description = "获取用户发送的好友请求列表")
    @GetMapping("/sent")
    public ApiResponse<List<ContactRequestResponse>> getSentRequests(
            @Parameter(description = "请求状态") @RequestParam(required = false) ContactRequestStatus status) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<ContactRequestResponse> requests = contactRequestService.getSentRequests(userId, status);
            return ApiResponse.success("获取发送的好友请求成功", requests);
        } catch (Exception e) {
            return ApiResponse.badRequest("获取发送的好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取好友请求详情
     */
    @Operation(summary = "获取好友请求详情", description = "获取指定好友请求的详细信息")
    @GetMapping("/{requestId}")
    public ApiResponse<ContactRequestResponse> getRequestDetail(
            @Parameter(description = "请求ID") @PathVariable Long requestId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        try {
            ContactRequestResponse request = contactRequestService.getRequestDetail(requestId, userId);
            return ApiResponse.success("获取好友请求详情成功", request);
        } catch (Exception e) {
            return ApiResponse.badRequest("获取好友请求详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取待处理请求数量
     */
    @Operation(summary = "获取待处理请求数量", description = "获取用户待处理的好友请求数量")
    @GetMapping("/pending/count")
    public ApiResponse<Long> getPendingRequestCount(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        try {
            long count = contactRequestService.getPendingRequestCount(userId);
            return ApiResponse.success("获取待处理请求数量成功", count);
        } catch (Exception e) {
            return ApiResponse.badRequest("获取待处理请求数量失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理好友请求
     */
    @Operation(summary = "批量处理好友请求", description = "批量接受或拒绝好友请求")
    @PutMapping("/batch")
    public ApiResponse<Integer> batchProcessRequests(
            @Parameter(description = "请求ID列表") @RequestBody List<Long> requestIds,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "是否接受") @RequestParam boolean accept) {
        try {
            int processedCount = contactRequestService.batchProcessRequests(requestIds, userId, accept);
            return ApiResponse.success("批量处理好友请求成功", processedCount);
        } catch (Exception e) {
            return ApiResponse.badRequest("批量处理好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的好友请求
     */
    @Operation(summary = "获取最近的好友请求", description = "获取用户最近收到的好友请求")
    @GetMapping("/recent")
    public ApiResponse<List<ContactRequestResponse>> getRecentRequests(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ContactRequestResponse> requests = contactRequestService.getRecentRequests(userId, limit);
            return ApiResponse.success("获取最近的好友请求成功", requests);
        } catch (Exception e) {
            return ApiResponse.badRequest("获取最近的好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 删除已处理的请求记录
     */
    @Operation(summary = "删除已处理的请求记录", description = "删除已处理的好友请求记录")
    @DeleteMapping("/{requestId}/record")
    public ApiResponse<Void> deleteProcessedRequest(
            @Parameter(description = "请求ID") @PathVariable Long requestId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        try {
            boolean success = contactRequestService.deleteProcessedRequest(requestId, userId);
            if (success) {
                return ApiResponse.success("删除已处理的请求记录成功", null);
            } else {
                return ApiResponse.badRequest("删除已处理的请求记录失败");
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("删除已处理的请求记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取好友请求统计信息
     */
    @Operation(summary = "获取好友请求统计信息", description = "获取用户的好友请求统计信息")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ContactRequestStatsResponse>> getRequestStats() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            ContactRequestStatsResponse stats = contactRequestService.getRequestStats(userId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "获取统计信息失败: " + e.getMessage()));
        }
    }
}