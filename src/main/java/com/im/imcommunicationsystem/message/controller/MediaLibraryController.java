package com.im.imcommunicationsystem.message.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.message.dto.request.MediaUploadRequest;
import com.im.imcommunicationsystem.message.dto.response.MediaResponse;
import com.im.imcommunicationsystem.message.service.MediaLibraryService;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

/**
 * 媒体库控制器
 * 处理媒体文件管理相关的HTTP请求
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaLibraryController {

    private final MediaLibraryService mediaLibraryService;
    private final SecurityUtils securityUtils;

    /**
     * 获取媒体库
     * 
     * @param conversationId 会话ID（可选）
     * @param mediaType 媒体类型（可选）
     * @param pageable 分页参数
     * @param authentication 认证信息
     * @return 媒体列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MediaResponse>>> getMediaLibrary(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) String mediaType,
            Pageable pageable,
            Authentication authentication) {
        
        log.info("接收到获取媒体库请求: conversationId={}, mediaType={}, page={}, size={}", 
                conversationId, mediaType, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 使用SecurityUtils获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            
            if (userId == null) {
                log.error("获取媒体库失败：无法获取用户ID");
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
            }
            
            log.info("当前用户ID: {}", userId);
            
            // 调用服务层获取媒体库
            Page<MediaResponse> mediaPage = mediaLibraryService.getMediaLibrary(conversationId, mediaType, pageable, userId);
            
            // 检查返回结果
            if (mediaPage == null || mediaPage.isEmpty()) {
                log.info("媒体库为空");
                return ResponseEntity.ok(ApiResponse.success(Page.empty()));
            }
            
            log.info("获取媒体库成功: 总数={}, 页码={}/{}, 当前页内容数量={}", 
                    mediaPage.getTotalElements(), 
                    mediaPage.getNumber() + 1, 
                    mediaPage.getTotalPages(),
                    mediaPage.getContent().size());
            
            // 记录第一个媒体项的信息（如果有）
            if (!mediaPage.getContent().isEmpty()) {
                MediaResponse firstItem = mediaPage.getContent().get(0);
                log.info("第一个媒体项: id={}, fileName={}, fileUrl={}", 
                        firstItem.getId(), firstItem.getFileName(), firstItem.getFileUrl());
            }
            
            return ResponseEntity.ok(ApiResponse.success(mediaPage));
            
        } catch (Exception e) {
            log.error("获取媒体库失败", e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("获取媒体库失败: " + e.getMessage()));
        }
    }

    /**
     * 上传媒体文件
     * 
     * @param file 文件
     * @param request 上传请求
     * @param authentication 认证信息
     * @return 媒体响应
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute MediaUploadRequest request,
            Authentication authentication) {
    
        log.info("接收到媒体文件上传请求: filename={}, contentType={}, size={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize());
    
        try {
            // 使用SecurityUtils获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            
            if (userId == null) {
                log.error("上传失败：无法获取用户ID");
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
            }
            
            log.info("当前用户ID: {}", userId);
            
            // 调用服务层处理上传
            MediaResponse mediaResponse = mediaLibraryService.uploadMedia(file, request, userId);
            
            log.info("媒体文件上传成功: id={}, url={}", mediaResponse.getId(), mediaResponse.getFileUrl());
            return ResponseEntity.ok(ApiResponse.success("媒体文件上传成功", mediaResponse));
            
        } catch (IllegalArgumentException e) {
            // 参数验证错误
            log.error("媒体文件上传参数错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
            
        } catch (Exception e) {
            // 其他错误
            log.error("媒体文件上传失败", e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("媒体文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取媒体文件
     * 
     * @param mediaId 媒体ID
     * @param authentication 认证信息
     * @return 媒体文件
     */
    @GetMapping("/files/{mediaId}")
    public ResponseEntity<?> getMediaFile(
            @PathVariable Long mediaId,
            Authentication authentication) {
        
        log.info("接收到获取媒体文件请求: mediaId={}", mediaId);
        
        try {
            // 使用SecurityUtils获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            
            if (userId == null) {
                log.error("获取媒体文件失败：无法获取用户ID");
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
            }
            
            // 获取媒体文件
            FileUpload fileUpload = mediaLibraryService.getMediaFileById(mediaId);
            
            if (fileUpload == null) {
                log.warn("媒体文件不存在: mediaId={}", mediaId);
                return ResponseEntity.notFound().build();
            }
            
            // 构建响应
            MediaResponse response = MediaResponse.builder()
                    .id(fileUpload.getId())
                    .fileName(fileUpload.getFileName())
                    .originalFileName(fileUpload.getOriginalName())
                    .fileType(fileUpload.getFileType().name())
                    .fileSize(fileUpload.getFileSize())
                    .fileUrl(fileUpload.getFileUrl())
                    .url(fileUpload.getFileUrl())
                    .uploadTime(fileUpload.getCreatedAt())
                    .uploaderId(fileUpload.getUserId())
                    .status("completed")
                    .build();
            
            log.info("媒体文件获取成功: id={}, url={}", fileUpload.getId(), fileUpload.getFileUrl());
            return ResponseEntity.ok(ApiResponse.success("媒体文件获取成功", response));
            
        } catch (Exception e) {
            log.error("获取媒体文件失败", e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("获取媒体文件失败: " + e.getMessage()));
        }
    }

    /**
     * 下载媒体文件
     * 
     * @param mediaId 媒体ID
     * @param authentication 认证信息
     * @return 文件流
     */
    @GetMapping("/{mediaId}/download")
    public ResponseEntity<byte[]> downloadMedia(
            @PathVariable Long mediaId,
            Authentication authentication) {
        // TODO: 实现下载媒体文件逻辑
        return ResponseEntity.ok().build();
    }

    /**
     * 直接获取媒体文件内容
     * 
     * @param mediaId 媒体ID
     * @param authentication 认证信息
     * @return 媒体文件内容
     */
    @GetMapping("/content/{mediaId}")
    public ResponseEntity<?> getMediaContent(
            @PathVariable Long mediaId,
            Authentication authentication) {
        
        log.info("接收到获取媒体文件内容请求: mediaId={}", mediaId);
        
        try {
            // 使用SecurityUtils获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            
            if (userId == null) {
                log.error("获取媒体文件内容失败：无法获取用户ID");
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录或会话已过期"));
            }
            
            // 获取媒体文件
            FileUpload fileUpload = mediaLibraryService.getMediaFileById(mediaId);
            
            if (fileUpload == null) {
                log.warn("媒体文件不存在: mediaId={}", mediaId);
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件内容
            byte[] fileContent = mediaLibraryService.downloadMedia(mediaId, userId);
            
            if (fileContent == null || fileContent.length == 0) {
                log.warn("媒体文件内容为空: mediaId={}", mediaId);
                return ResponseEntity.noContent().build();
            }
            
            // 设置响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileUpload.getMimeType() != null ? fileUpload.getMimeType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileUpload.getFileName() + "\"")
                    .body(fileContent);
            
        } catch (Exception e) {
            log.error("获取媒体文件内容失败", e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("获取媒体文件内容失败: " + e.getMessage()));
        }
    }

    /**
     * 直接获取媒体文件内容（无需认证）
     * 
     * @param mediaId 媒体ID
     * @return 媒体文件内容
     */
    @GetMapping("/public/content/{mediaId}")
    public ResponseEntity<?> getPublicMediaContent(@PathVariable Long mediaId) {
        
        log.info("接收到获取公开媒体文件内容请求: mediaId={}", mediaId);
        
        try {
            // 获取媒体文件
            FileUpload fileUpload = mediaLibraryService.getMediaFileById(mediaId);
            
            if (fileUpload == null) {
                log.warn("媒体文件不存在: mediaId={}", mediaId);
                return ResponseEntity.notFound().build();
            }
            
            // 记录文件信息
            log.info("找到媒体文件: id={}, fileName={}, fileType={}, accessLevel={}, userId={}, bucketName={}, filePath={}, fileUrl={}",
                    fileUpload.getId(), fileUpload.getFileName(), fileUpload.getFileType(),
                    fileUpload.getAccessLevel(), fileUpload.getUserId(), 
                    fileUpload.getBucketName(), fileUpload.getFilePath(), fileUpload.getFileUrl());
            
            // 检查文件是否已删除
            if (fileUpload.getIsDeleted()) {
                log.warn("媒体文件已被删除: mediaId={}", mediaId);
                return ResponseEntity.status(410).body(ApiResponse.error(410, "媒体文件已被删除"));
            }
            
            // 使用文件所有者的用户ID
            Long ownerId = fileUpload.getUserId();
            log.info("使用文件所有者ID获取媒体内容: ownerId={}", ownerId);
            
            // 获取文件内容
            byte[] fileContent = mediaLibraryService.downloadMedia(mediaId, ownerId);
            
            if (fileContent == null || fileContent.length == 0) {
                log.warn("媒体文件内容为空: mediaId={}", mediaId);
                return ResponseEntity.noContent().build();
            }
            
            log.info("成功获取媒体文件内容: mediaId={}, contentLength={} bytes", mediaId, fileContent.length);
            
            // 设置响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileUpload.getMimeType() != null ? fileUpload.getMimeType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileUpload.getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 缓存一年
                    .header("Access-Control-Allow-Origin", "*") // 允许跨域访问
                    .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept")
                    .header("Access-Control-Max-Age", "3600")
                    .body(fileContent);
            
        } catch (Exception e) {
            log.error("获取媒体文件内容失败: mediaId={}", mediaId, e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("获取媒体文件内容失败: " + e.getMessage()));
        }
    }
}