package com.im.imcommunicationsystem.moment.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse.UserBriefInfo;
import com.im.imcommunicationsystem.moment.service.MomentLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 动态点赞控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moments")
public class MomentLikeController {

    private final MomentLikeService momentLikeService;

    /**
     * 点赞动态
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 操作结果
     */
    @PostMapping("/{momentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> likeMoment(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("用户 {} 点赞动态 {}", userId, momentId);
        
        boolean success = momentLikeService.likeMoment(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success ? "点赞成功" : "已经点赞过了", success));
    }

    /**
     * 取消点赞
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 操作结果
     */
    @PostMapping("/{momentId}/unlike")
    public ResponseEntity<ApiResponse<Boolean>> unlikeMoment(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("用户 {} 取消点赞动态 {}", userId, momentId);
        
        boolean success = momentLikeService.unlikeMoment(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success ? "取消点赞成功" : "没有点赞过", success));
    }

    /**
     * 获取动态点赞列表
     *
     * @param momentId 动态ID
     * @param page 页码
     * @param size 每页大小
     * @param userDetails 当前用户
     * @return 点赞用户列表
     */
    @GetMapping("/{momentId}/likes")
    public ResponseEntity<ApiResponse<Page<UserBriefInfo>>> getMomentLikes(
            @PathVariable Long momentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("获取动态 {} 的点赞列表, 请求者: {}", momentId, userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserBriefInfo> likes = momentLikeService.getMomentLikes(momentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(likes));
    }

    /**
     * 检查用户是否已点赞
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 是否已点赞
     */
    @GetMapping("/{momentId}/likes/check")
    public ResponseEntity<ApiResponse<Boolean>> checkUserLiked(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("检查用户 {} 是否点赞动态 {}", userId, momentId);
        
        boolean liked = momentLikeService.checkUserLiked(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    /**
     * 从UserDetails中提取用户ID
     *
     * @param userDetails 用户详情
     * @return 用户ID
     */
    private Long extractUserId(UserDetails userDetails) {
        // 首先尝试从认证信息中获取
        if (userDetails != null) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.warn("无法从UserDetails中解析用户ID: {}", userDetails.getUsername());
            }
        }
        
        // 尝试从请求头中获取
        HttpServletRequest request = 
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("无法从X-User-Id头中解析用户ID: {}", userIdHeader);
            }
        }
        
        // 尝试从请求参数中获取
        String userIdParam = request.getParameter("userId");
        if (userIdParam != null && !userIdParam.isEmpty()) {
            try {
                return Long.parseLong(userIdParam);
            } catch (NumberFormatException e) {
                log.warn("无法从userId参数中解析用户ID: {}", userIdParam);
            }
        }
        
        // 最后从请求属性中获取（由JwtAuthenticationFilter设置）
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            if (userIdAttr instanceof Long) {
                return (Long) userIdAttr;
            } else if (userIdAttr instanceof String) {
                try {
                    return Long.parseLong((String) userIdAttr);
                } catch (NumberFormatException e) {
                    log.warn("无法从请求属性中解析用户ID: {}", userIdAttr);
                }
            }
        }
        
        // 如果都未找到，返回1L作为默认值（仅用于开发测试）
        log.warn("未能从任何地方获取到用户ID，使用默认值1L");
        return 1L;
    }
} 