package com.im.imcommunicationsystem.relationship.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagUpdateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.service.ContactTagService;
import com.im.imcommunicationsystem.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 标签管理控制器
 */
@Tag(name = "标签管理", description = "标签相关接口")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {

    private final ContactTagService contactTagService;
    private final JwtUtils jwtUtils;

    /**
     * 获取用户的所有标签
     */
    @Operation(summary = "获取用户标签列表", description = "获取当前用户的所有标签")
    @GetMapping
    public ApiResponse<List<ContactTagResponse>> getUserTags(HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("获取用户标签列表请求: userId={}", userId);
            List<ContactTagResponse> tags = contactTagService.getUserTags(userId);
            return ApiResponse.success(tags);
        } catch (Exception e) {
            log.error("获取用户标签列表失败: error={}", e.getMessage(), e);
            return ApiResponse.error(500, "获取标签列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建标签
     */
    @Operation(summary = "创建标签", description = "创建新的标签")
    @PostMapping
    public ApiResponse<ContactTagResponse> createTag(@Valid @RequestBody ContactTagCreateRequest request, HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            // 设置用户ID到请求中
            request.setUserId(userId);
            
            log.info("创建标签请求: userId={}, name={}, color={}", userId, request.getName(), request.getColor());
            ContactTagResponse tag = contactTagService.createTag(request);
            return ApiResponse.success(tag);
        } catch (Exception e) {
            log.error("创建标签失败: error={}", e.getMessage(), e);
            return ApiResponse.error(500, "创建标签失败: " + e.getMessage());
        }
    }

    /**
     * 更新标签
     */
    @Operation(summary = "更新标签", description = "更新指定的标签")
    @PutMapping("/{tagId}")
    public ApiResponse<ContactTagResponse> updateTag(@PathVariable Long tagId, @Valid @RequestBody ContactTagUpdateRequest request, HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("更新标签请求: userId={}, tagId={}, name={}, color={}", userId, tagId, request.getName(), request.getColor());
            ContactTagResponse tag = contactTagService.updateTag(tagId, request, userId);
            return ApiResponse.success(tag);
        } catch (Exception e) {
            log.error("更新标签失败: tagId={}, error={}", tagId, e.getMessage(), e);
            return ApiResponse.error(500, "更新标签失败: " + e.getMessage());
        }
    }

    /**
     * 删除标签
     */
    @Operation(summary = "删除标签", description = "删除指定的标签")
    @DeleteMapping("/{tagId}")
    public ApiResponse<Boolean> deleteTag(@PathVariable Long tagId, HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取用户ID
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("删除标签请求: userId={}, tagId={}", userId, tagId);
            boolean success = contactTagService.deleteTag(tagId, userId);
            return ApiResponse.success(success);
        } catch (Exception e) {
            log.error("删除标签失败: tagId={}, error={}", tagId, e.getMessage(), e);
            return ApiResponse.error(500, "删除标签失败: " + e.getMessage());
        }
    }
}