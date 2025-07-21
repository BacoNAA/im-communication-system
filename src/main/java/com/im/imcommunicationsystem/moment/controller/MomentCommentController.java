package com.im.imcommunicationsystem.moment.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.moment.dto.request.CreateCommentRequest;
import com.im.imcommunicationsystem.moment.dto.response.CommentResponse;
import com.im.imcommunicationsystem.moment.service.MomentCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.im.imcommunicationsystem.moment.exception.MomentNotFoundException;

/**
 * 动态评论控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moments")
public class MomentCommentController {

    private final MomentCommentService commentService;

    /**
     * 创建评论
     *
     * @param momentId 动态ID
     * @param request 创建评论请求
     * @param userDetails 当前用户
     * @return 创建的评论
     */
    @PostMapping("/{momentId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long momentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
        Long userId = extractUserId(userDetails);
            log.info("用户 {} 评论动态 {}, 请求内容: {}", userId, momentId, request);
            
            if (userId == null) {
                log.error("评论动态失败: 用户ID为null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
        
        CommentResponse comment = commentService.createComment(momentId, userId, request);
            log.info("评论创建成功，ID: {}", comment.getId());
            
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("评论成功", comment));
        } catch (MomentNotFoundException e) {
            log.error("评论动态失败: 动态不存在, momentId: {}", momentId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "动态不存在"));
        } catch (IllegalArgumentException e) {
            log.error("评论动态失败: 参数错误, momentId: {}, error: {}", momentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("评论动态失败: 服务器错误, momentId: {}", momentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "服务器错误，请稍后重试"));
        }
    }

    /**
     * 回复评论
     *
     * @param momentId 动态ID
     * @param commentId 评论ID
     * @param request 创建评论请求
     * @param userDetails 当前用户
     * @return 创建的回复
     */
    @PostMapping("/{momentId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @PathVariable Long momentId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("用户 {} 回复评论 {}", userId, commentId);
        
        CommentResponse reply = commentService.replyToComment(momentId, commentId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("回复成功", reply));
    }

    /**
     * 获取动态评论列表
     *
     * @param momentId 动态ID
     * @param page 页码
     * @param size 每页大小
     * @param userDetails 当前用户
     * @return 评论分页数据
     */
    @GetMapping("/{momentId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMomentComments(
            @PathVariable Long momentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("获取动态 {} 的评论列表, 请求者: {}", momentId, userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponse> comments = commentService.getMomentComments(momentId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取动态评论及回复
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 评论列表（包含回复）
     */
    @GetMapping("/{momentId}/comments/with-replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getMomentCommentsWithReplies(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("获取动态 {} 的评论及回复, 请求者: {}", momentId, userId);
        
        List<CommentResponse> comments = commentService.getMomentCommentsWithReplies(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 删除评论
     *
     * @param momentId 动态ID
     * @param commentId 评论ID
     * @param userDetails 当前用户
     * @return 操作结果
     */
    @DeleteMapping("/{momentId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteComment(
            @PathVariable Long momentId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("用户 {} 删除评论 {}", userId, commentId);
        
        boolean success = commentService.deleteComment(commentId, userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("评论删除成功", true));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "无权删除该评论"));
        }
    }

    /**
     * 从UserDetails中提取用户ID
     *
     * @param userDetails 用户详情
     * @return 用户ID
     */
    private Long extractUserId(UserDetails userDetails) {
        try {
            // 从请求头中获取用户ID
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String userIdHeader = request.getHeader("X-User-Id");
            
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    return Long.parseLong(userIdHeader);
                } catch (NumberFormatException e) {
                    log.error("解析X-User-Id请求头失败: {}", userIdHeader, e);
                }
            }
            
            // 如果请求头中没有用户ID，尝试从UserDetails获取
            if (userDetails != null) {
                // 这里根据你的UserDetails实现来获取用户ID
                if (userDetails.getUsername() != null && userDetails.getUsername().matches("\\d+")) {
                    return Long.parseLong(userDetails.getUsername());
                }
            }
            
            log.warn("无法获取用户ID，返回默认值1L");
            return 1L;
        } catch (Exception e) {
            log.error("获取用户ID时发生异常", e);
        return 1L;
        }
    }
} 