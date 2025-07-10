package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.service.PublicFileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开文件上传控制器
 * 专门处理公开访问的文件（如头像、公开图片等）
 */
@RestController
@RequestMapping("/api/public-files")
@Slf4j
@RequiredArgsConstructor
public class PublicFileUploadController {

    private final PublicFileUploadService publicFileUploadService;
    private final SecurityUtils securityUtils;

    /**
     * 上传公开文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadPublicFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            log.info("开始上传公开文件: userId={}, fileName={}, fileSize={}", 
                    userId, file.getOriginalFilename(), file.getSize());
            
            String fileUrl = publicFileUploadService.uploadFile(file, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            
            log.info("公开文件上传成功: userId={}, fileUrl={}", userId, fileUrl);
            
            return ApiResponse.success("公开文件上传成功", result);
            
        } catch (Exception e) {
            log.error("公开文件上传失败: fileName={}", file.getOriginalFilename(), e);
            return ApiResponse.serverError("公开文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传公开图片（含压缩）
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadPublicImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "maxWidth", defaultValue = "800") int maxWidth,
            @RequestParam(value = "maxHeight", defaultValue = "600") int maxHeight,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            log.info("开始上传公开图片: userId={}, fileName={}, maxWidth={}, maxHeight={}", 
                    userId, file.getOriginalFilename(), maxWidth, maxHeight);
            
            String imageUrl = publicFileUploadService.uploadImage(file, userId, maxWidth, maxHeight);
            
            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("originalSize", file.getSize());
            result.put("maxWidth", maxWidth);
            result.put("maxHeight", maxHeight);
            
            log.info("公开图片上传成功: userId={}, imageUrl={}", userId, imageUrl);
            
            return ApiResponse.success("公开图片上传成功", result);
            
        } catch (Exception e) {
            log.error("公开图片上传失败: fileName={}", file.getOriginalFilename(), e);
            return ApiResponse.serverError("公开图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像
     */
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            log.info("开始上传用户头像: userId={}, fileName={}", userId, file.getOriginalFilename());
            
            String avatarUrl = publicFileUploadService.uploadAvatar(file, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("originalSize", file.getSize());
            
            log.info("用户头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
            
            return ApiResponse.success("头像上传成功", result);
            
        } catch (Exception e) {
            log.error("头像上传失败: userId={}, fileName={}", 
                    securityUtils.getCurrentUserId(), file.getOriginalFilename(), e);
            return ApiResponse.serverError("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除公开文件（软删除）
     */
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deletePublicFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            log.info("开始删除公开文件: userId={}, fileId={}", userId, fileId);
            
            boolean success = publicFileUploadService.deleteFile(fileId, userId);
            
            if (success) {
                log.info("公开文件删除成功: userId={}, fileId={}", userId, fileId);
                return ApiResponse.<Void>success("公开文件删除成功", null);
            } else {
                return ApiResponse.serverError("公开文件删除失败");
            }
            
        } catch (Exception e) {
            log.error("公开文件删除失败: fileId={}", fileId, e);
            return ApiResponse.serverError("公开文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 物理删除公开文件
     */
    @DeleteMapping("/{fileId}/physical")
    public ApiResponse<Void> physicalDeletePublicFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            log.info("开始物理删除公开文件: userId={}, fileId={}", userId, fileId);
            
            boolean success = publicFileUploadService.physicalDeleteFile(fileId, userId);
            
            if (success) {
                log.info("公开文件物理删除成功: userId={}, fileId={}", userId, fileId);
                return ApiResponse.<Void>success("公开文件物理删除成功", null);
            } else {
                return ApiResponse.serverError("公开文件物理删除失败");
            }
            
        } catch (Exception e) {
            log.error("公开文件物理删除失败: fileId={}", fileId, e);
            return ApiResponse.serverError("公开文件物理删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的公开文件列表
     */
    @GetMapping("/my-files")
    public ApiResponse<List<FileUpload>> getMyPublicFiles(
            @RequestParam(value = "fileType", required = false) String fileType,
            Authentication authentication) {
        
        try {
            Long userId = securityUtils.getCurrentUserId();
            
            FileUpload.FileType type = null;
            if (fileType != null && !fileType.trim().isEmpty()) {
                try {
                    type = FileUpload.FileType.valueOf(fileType.toLowerCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.badRequest("无效的文件类型: " + fileType);
                }
            }
            
            List<FileUpload> files = publicFileUploadService.getUserPublicFiles(userId, type);
            
            log.info("获取用户公开文件列表成功: userId={}, fileType={}, count={}", 
                    userId, fileType, files.size());
            
            return ApiResponse.success("获取公开文件列表成功", files);
            
        } catch (Exception e) {
            log.error("获取用户公开文件列表失败", e);
            return ApiResponse.serverError("获取公开文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取公开文件信息
     */
    @GetMapping("/{fileId}")
    public ApiResponse<FileUpload> getPublicFileInfo(@PathVariable Long fileId) {
        
        try {
            FileUpload fileUpload = publicFileUploadService.getPublicFileById(fileId);
            
            if (fileUpload != null) {
                log.info("获取公开文件信息成功: fileId={}", fileId);
                return ApiResponse.success("获取公开文件信息成功", fileUpload);
            } else {
                return ApiResponse.notFound("公开文件不存在或已被删除");
            }
            
        } catch (Exception e) {
            log.error("获取公开文件信息失败: fileId={}", fileId, e);
            return ApiResponse.serverError("获取公开文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取公开文件访问URL
     */
    @GetMapping("/{fileId}/url")
    public ApiResponse<Map<String, String>> getPublicFileUrl(@PathVariable Long fileId) {
        
        try {
            String fileUrl = publicFileUploadService.getPublicFileUrl(fileId);
            
            if (fileUrl != null) {
                Map<String, String> result = new HashMap<>();
                result.put("fileUrl", fileUrl);
                
                log.info("获取公开文件URL成功: fileId={}", fileId);
                return ApiResponse.success("获取公开文件URL成功", result);
            } else {
                return ApiResponse.notFound("公开文件不存在或已被删除");
            }
            
        } catch (Exception e) {
            log.error("获取公开文件URL失败: fileId={}", fileId, e);
            return ApiResponse.serverError("获取公开文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否为公开文件
     */
    @GetMapping("/{fileId}/check")
    public ApiResponse<Map<String, Boolean>> checkPublicFile(@PathVariable Long fileId) {
        
        try {
            boolean isPublic = publicFileUploadService.isPublicFile(fileId);
            
            Map<String, Boolean> result = new HashMap<>();
            result.put("isPublic", isPublic);
            
            log.info("检查公开文件状态成功: fileId={}, isPublic={}", fileId, isPublic);
            return ApiResponse.success("检查公开文件状态成功", result);
            
        } catch (Exception e) {
            log.error("检查公开文件状态失败: fileId={}", fileId, e);
            return ApiResponse.serverError("检查公开文件状态失败: " + e.getMessage());
        }
    }
}