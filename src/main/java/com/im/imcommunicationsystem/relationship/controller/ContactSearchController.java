package com.im.imcommunicationsystem.relationship.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.relationship.dto.request.SearchContactRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactSearchResponse;
import com.im.imcommunicationsystem.relationship.service.ContactSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * 联系人搜索控制器
 * 专门处理用户搜索和添加好友相关功能
 */
@Tag(name = "联系人搜索", description = "用户搜索和添加好友相关接口")
@RestController
@RequestMapping("/api/contact-search")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ContactSearchController {

    private final ContactSearchService contactSearchService;

    /**
     * 根据用户ID精确搜索用户
     */
    @Operation(summary = "根据用户ID搜索", description = "根据用户ID精确搜索用户")
    @GetMapping("/by-user-id")
    public ApiResponse<ContactSearchResponse> searchByUserId(
            @Parameter(description = "用户ID字符串") @RequestParam @NotBlank String userIdStr,
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long currentUserId) {
        try {
            log.info("根据用户ID搜索: userIdStr={}, currentUserId={}", userIdStr, currentUserId);
            
            SearchContactRequest searchRequest = SearchContactRequest.builder()
                    .keyword(userIdStr)
                    .userId(currentUserId)
                    .build();
            
            Optional<ContactSearchResponse> result = contactSearchService.searchUserById(searchRequest);
            if (result.isPresent()) {
                return ApiResponse.success(result.get());
            } else {
                return ApiResponse.error(404, "未找到用户或用户不允许被搜索");
            }
        } catch (Exception e) {
            log.error("根据用户ID搜索失败: userIdStr={}, currentUserId={}, error={}", userIdStr, currentUserId, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 根据昵称模糊搜索用户
     */
    @Operation(summary = "根据昵称搜索", description = "根据昵称模糊搜索用户")
    @GetMapping("/by-nickname")
    public ApiResponse<List<ContactSearchResponse>> searchByNickname(
            @Parameter(description = "昵称关键词") @RequestParam @NotBlank String nickname,
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long currentUserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("根据昵称搜索: nickname={}, currentUserId={}, page={}, size={}", nickname, currentUserId, page, size);
            
            SearchContactRequest searchRequest = SearchContactRequest.builder()
                    .keyword(nickname)
                    .userId(currentUserId)
                    .build();
            
            List<ContactSearchResponse> results = contactSearchService.searchUserByNickname(searchRequest);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("根据昵称搜索失败: nickname={}, currentUserId={}, error={}", nickname, currentUserId, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 通用搜索用户
     */
    @Operation(summary = "通用搜索", description = "根据关键词搜索用户（支持用户ID和昵称）")
    @GetMapping("/search")
    public ApiResponse<List<ContactSearchResponse>> searchUsers(
            @Parameter(description = "搜索关键词") @RequestParam @NotBlank String keyword,
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long currentUserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("通用搜索用户: keyword={}, currentUserId={}, page={}, size={}", keyword, currentUserId, page, size);
            
            SearchContactRequest searchRequest = SearchContactRequest.builder()
                    .keyword(keyword)
                    .userId(currentUserId)
                    .build();
            
            List<ContactSearchResponse> results = contactSearchService.searchUsers(searchRequest);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("通用搜索失败: keyword={}, currentUserId={}, error={}", keyword, currentUserId, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 根据二维码搜索用户
     */
    @Operation(summary = "二维码搜索", description = "根据二维码数据搜索用户")
    @PostMapping("/by-qrcode")
    public ApiResponse<ContactSearchResponse> searchByQRCode(
            @Parameter(description = "二维码数据") @RequestParam @NotBlank String qrCodeData,
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long currentUserId) {
        try {
            log.info("根据二维码搜索: currentUserId={}", currentUserId);
            
            Optional<ContactSearchResponse> result = contactSearchService.searchUserByQRCode(qrCodeData, currentUserId);
            if (result.isPresent()) {
                return ApiResponse.success(result.get());
            } else {
                return ApiResponse.error(404, "二维码无效或用户不存在");
            }
        } catch (Exception e) {
            log.error("根据二维码搜索失败: currentUserId={}, error={}", currentUserId, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户关系状态
     */
    @Operation(summary = "检查关系状态", description = "检查当前用户与目标用户的关系状态")
    @GetMapping("/relationship-status")
    public ApiResponse<String> getRelationshipStatus(
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long userId,
            @Parameter(description = "目标用户ID") @RequestParam @NotNull Long targetUserId) {
        try {
            log.debug("检查关系状态: userId={}, targetUserId={}", userId, targetUserId);
            
            String status = contactSearchService.checkRelationshipStatus(userId, targetUserId);
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("检查关系状态失败: userId={}, targetUserId={}, error={}", userId, targetUserId, e.getMessage(), e);
            return ApiResponse.error(500, "检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查搜索权限
     */
    @Operation(summary = "检查搜索权限", description = "检查是否可以搜索指定用户")
    @GetMapping("/can-search")
    public ApiResponse<Boolean> canSearchUser(
            @Parameter(description = "当前用户ID") @RequestParam @NotNull Long userId,
            @Parameter(description = "目标用户ID") @RequestParam @NotNull Long targetUserId) {
        try {
            log.debug("检查搜索权限: userId={}, targetUserId={}", userId, targetUserId);
            
            boolean canSearch = contactSearchService.validateSearchPermission(targetUserId, userId);
            return ApiResponse.success(canSearch);
        } catch (Exception e) {
            log.error("检查搜索权限失败: userId={}, targetUserId={}, error={}", userId, targetUserId, e.getMessage(), e);
            return ApiResponse.error(500, "检查失败: " + e.getMessage());
        }
    }

    /**
     * 使用请求对象进行搜索（备用接口）
     */
    @Operation(summary = "搜索用户（请求对象）", description = "使用请求对象进行用户搜索")
    @PostMapping("/search-with-request")
    public ApiResponse<List<ContactSearchResponse>> searchWithRequest(
            @Valid @RequestBody SearchContactRequest request) {
        try {
            log.info("使用请求对象搜索: request={}", request);
            
            List<ContactSearchResponse> results = contactSearchService.searchUsers(request);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("使用请求对象搜索失败: request={}, error={}", request, e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }
}