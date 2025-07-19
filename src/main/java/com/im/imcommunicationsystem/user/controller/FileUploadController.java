package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.service.impl.MinioFileUploadServiceImpl;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文件上传控制器
 * 提供文件上传、下载、删除等API接口
 */
@RestController
@RequestMapping("/api/files")
@Slf4j
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioFileUploadServiceImpl fileUploadService;
    private final SecurityUtils securityUtils;
    private final FileUploadRepository fileUploadRepository;

    /**
     * 上传文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 上传文件
            FileUpload fileUpload = fileUploadService.uploadFile(file, userId);
            
            // 构建响应数据
            Map<String, Object> result = buildFileResponse(fileUpload);
            
            log.info("文件上传成功，用户ID: {}, 文件ID: {}", userId, fileUpload.getId());
            return ApiResponse.success("文件上传成功", result);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.serverError("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传图片（带压缩）
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "maxWidth", defaultValue = "1920") int maxWidth,
            @RequestParam(value = "maxHeight", defaultValue = "1080") int maxHeight,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 上传图片
            FileUpload fileUpload = fileUploadService.uploadImage(file, userId, maxWidth, maxHeight);
            
            // 构建响应数据
            Map<String, Object> result = buildFileResponse(fileUpload);
            
            log.info("图片上传成功，用户ID: {}, 文件ID: {}", userId, fileUpload.getId());
            return ApiResponse.success("图片上传成功", result);
            
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ApiResponse.serverError("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（软删除）
     */
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(
            @PathVariable String fileId,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 删除文件
            boolean success = fileUploadService.deleteFile(fileId, userId);
            
            if (success) {
                log.info("文件删除成功，用户ID: {}, 文件ID: {}", userId, fileId);
                return ApiResponse.success();
            } else {
                return ApiResponse.serverError("文件删除失败");
            }
            
        } catch (Exception e) {
            log.error("文件删除失败，文件ID: {}", fileId, e);
            return ApiResponse.serverError("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 物理删除文件（彻底删除）
     */
    @DeleteMapping("/{fileId}/physical")
    public ApiResponse<String> physicalDeleteFile(
            @PathVariable String fileId,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 物理删除文件
            boolean success = fileUploadService.physicalDeleteFile(fileId, userId);
            
            if (success) {
                log.info("文件物理删除成功，用户ID: {}, 文件ID: {}", userId, fileId);
                return ApiResponse.success("文件已彻底删除");
            } else {
                return ApiResponse.serverError("文件物理删除失败");
            }
            
        } catch (Exception e) {
            log.error("文件物理删除失败，文件ID: {}", fileId, e);
            return ApiResponse.serverError("文件物理删除失败: " + e.getMessage());
        }
    }

    /**
     * 恢复已删除的文件
     */
    @PostMapping("/{fileId}/restore")
    public ApiResponse<Void> restoreFile(
            @PathVariable String fileId,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 恢复文件
            boolean success = fileUploadService.restoreFile(fileId, userId);
            
            if (success) {
                log.info("文件恢复成功，用户ID: {}, 文件ID: {}", userId, fileId);
                return ApiResponse.success();
            } else {
                return ApiResponse.serverError("文件恢复失败");
            }
            
        } catch (Exception e) {
            log.error("文件恢复失败，文件ID: {}", fileId, e);
            return ApiResponse.serverError("文件恢复失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期的已删除文件
     * 管理员功能：清理指定天数之前软删除的文件
     */
    @PostMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanupExpiredFiles(
            @RequestParam(value = "days", defaultValue = "30") int days,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 这里可以添加管理员权限检查
            // 暂时允许所有用户执行清理操作
            
            // 清理过期文件
            int deletedCount = fileUploadService.cleanupExpiredFiles(days);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("cleanupDays", days);
            
            log.info("清理过期文件完成，用户ID: {}, 清理天数: {}, 删除数量: {}", userId, days, deletedCount);
            return ApiResponse.success("清理完成", result);
            
        } catch (Exception e) {
            log.error("清理过期文件失败", e);
            return ApiResponse.serverError("清理过期文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public ApiResponse<Map<String, Object>> getFileInfo(@PathVariable String fileId) {
        try {
            Optional<FileUpload> fileOptional = fileUploadService.getFileInfo(fileId);
            
            if (fileOptional.isPresent()) {
                Map<String, Object> result = buildFileResponse(fileOptional.get());
                return ApiResponse.success("获取文件信息成功", result);
            } else {
                return ApiResponse.notFound("文件不存在");
            }
            
        } catch (Exception e) {
            log.error("获取文件信息失败，文件ID: {}", fileId, e);
            return ApiResponse.serverError("获取文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getUserFiles(
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            log.info("获取文件列表请求 - 用户ID: {}, 文件类型: {}, 页码: {}, 大小: {}", userId, fileType, page, size);
            if (userId == null) {
                log.warn("用户未登录，无法获取文件列表");
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 解析文件类型
            FileUpload.FileType type = null;
            if (fileType != null && !fileType.trim().isEmpty()) {
                try {
                    type = FileUpload.FileType.valueOf(fileType.toLowerCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.badRequest("不支持的文件类型: " + fileType);
                }
            }
            
            // 获取文件列表
            List<FileUpload> files = fileUploadService.getUserFiles(userId, type, page, size);
            log.info("查询到文件数量: {}", files.size());
            if (files.isEmpty()) {
                log.warn("用户 {} 没有找到任何文件，文件类型: {}", userId, type);
            } else {
                log.info("用户 {} 找到 {} 个文件", userId, files.size());
            }
            
            // 构建响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("files", files.stream().map(this::buildFileResponse).toList());
            result.put("page", page);
            result.put("size", size);
            result.put("total", files.size());
            
            return ApiResponse.success("获取文件列表成功", result);
            
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return ApiResponse.serverError("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户文件统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserFileStats(Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponse.unauthorized("用户未登录");
            }
            
            // 获取统计信息
            Map<String, Object> stats = fileUploadService.getUserFileStats(userId);
            
            return ApiResponse.success("获取文件统计信息成功", stats);
            
        } catch (Exception e) {
            log.error("获取文件统计信息失败", e);
            return ApiResponse.serverError("获取文件统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId, Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            log.info("用户 {} 请求下载文件: {}", userId, fileId);
            
            // 获取文件信息
            Optional<FileUpload> fileOptional = fileUploadService.getFileInfo(fileId);
            if (fileOptional.isEmpty()) {
                log.warn("文件不存在: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            FileUpload fileUpload = fileOptional.get();
            
            // 检查文件所有权
            if (!fileUpload.getUserId().equals(userId)) {
                log.warn("用户 {} 尝试下载不属于自己的文件: {}", userId, fileId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 检查文件是否已删除
            if (fileUpload.getIsDeleted()) {
                log.warn("文件已被删除: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件流
            InputStream fileStream = fileUploadService.downloadFile(fileId, userId);
            if (fileStream == null) {
                log.error("无法获取文件流: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            // 创建资源
            InputStreamResource resource = new InputStreamResource(fileStream);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileUpload.getOriginalName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, fileUpload.getContentType());
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileUpload.getFileSize()));
            
            log.info("文件下载成功: {}, 用户: {}", fileId, userId);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("下载文件失败，文件ID: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取支持的文件类型
     */
    @GetMapping("/types")
    public ApiResponse<Map<String, Object>> getSupportedFileTypes() {
        Map<String, Object> types = new HashMap<>();
        
        Map<String, String[]> fileTypes = new HashMap<>();
        fileTypes.put("IMAGE", new String[]{"jpg", "jpeg", "png", "gif", "bmp", "webp"});
        fileTypes.put("VIDEO", new String[]{"mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"});
        fileTypes.put("AUDIO", new String[]{"mp3", "wav", "flac", "aac", "ogg", "wma"});
        fileTypes.put("DOCUMENT", new String[]{"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"});
        
        types.put("supportedTypes", fileTypes);
        types.put("maxFileSize", "50MB");
        types.put("maxImageSize", "1920x1080");
        
        return ApiResponse.success("获取支持的文件类型成功", types);
    }

    // ==================== 私有辅助方法 ====================



    /**
     * 构建文件响应数据
     */
    private Map<String, Object> buildFileResponse(FileUpload fileUpload) {
        Map<String, Object> result = new HashMap<>();
        
        // 确保fileId不为null
        Long fileId = fileUpload.getId();
        if (fileId == null) {
            log.warn("文件对象的ID为null: {}", fileUpload);
            return result; // 返回空的result，前端会跳过这个文件
        }
        
        result.put("fileId", fileId);
        result.put("originalFilename", fileUpload.getOriginalName());
        result.put("storedFilename", fileUpload.getFileName());
        result.put("fileUrl", fileUpload.getFileUrl());
        result.put("fileSize", fileUpload.getFileSize());
        result.put("mimeType", fileUpload.getContentType());
        result.put("fileType", fileUpload.getFileType());
        result.put("md5Hash", fileUpload.getMd5Hash());
        result.put("storageType", fileUpload.getStorageType());
        result.put("isPublic", fileUpload.getIsPublic());
        result.put("createdAt", fileUpload.getCreatedAt());
        
        // 图片特有信息
        if (fileUpload.getFileType() == FileUpload.FileType.image) {
            result.put("width", fileUpload.getWidth());
            result.put("height", fileUpload.getHeight());
            result.put("thumbnailUrl", fileUpload.getThumbnailUrl());
        }
        
        // 视频特有信息
        if (fileUpload.getFileType() == FileUpload.FileType.video) {
            result.put("duration", fileUpload.getDuration());
            result.put("width", fileUpload.getWidth());
            result.put("height", fileUpload.getHeight());
        }
        
        return result;
    }
}