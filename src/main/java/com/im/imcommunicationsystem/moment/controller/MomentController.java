package com.im.imcommunicationsystem.moment.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.moment.dto.request.CreateMomentRequest;
import com.im.imcommunicationsystem.moment.dto.response.MomentDetailResponse;
import com.im.imcommunicationsystem.moment.dto.response.MomentResponse;
import com.im.imcommunicationsystem.moment.service.MomentService;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.service.FileUploadService;
import com.im.imcommunicationsystem.user.service.PublicFileUploadService;
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
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moments")
public class MomentController {

    private final MomentService momentService;
    private final FileUploadService fileUploadService;
    private final PublicFileUploadService publicFileUploadService;

    /**
     * 创建动态
     *
     * @param request 创建动态请求
     * @param userDetails 当前用户
     * @return 创建的动态
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MomentResponse>> createMoment(
            @Valid @RequestBody CreateMomentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            log.info("用户 {} 创建新动态，请求内容: {}", userId, request);
            
            if (userId == null) {
                log.error("创建动态失败: 用户ID为null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
            
            MomentResponse moment = momentService.createMoment(userId, request);
            log.info("动态创建成功，ID: {}", moment.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("动态创建成功", moment));
        } catch (Exception e) {
            log.error("创建动态时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "创建动态失败: " + e.getMessage()));
        }
    }

    /**
     * 上传媒体文件
     *
     * @param files 媒体文件列表
     * @param userDetails 当前用户
     * @return 媒体URL列表
     */
    @PostMapping("/upload/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            log.info("用户 {} 上传 {} 张图片", userId, files.size());
            
            if (userId == null) {
                log.error("上传图片失败: 用户ID为null，可能未通过认证");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
            
            if (files == null || files.isEmpty()) {
                log.error("上传图片失败: 文件列表为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "未提供有效的图片文件"));
            }
            
            // 使用PublicFileUploadService上传图片
            List<String> mediaUrls = new ArrayList<>();
            int index = 0;
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("跳过空文件: index={}", index++);
                    continue;
                }
                
                log.info("上传图片: index={}, 文件名={}, 大小={}字节, 类型={}", 
                    index++, file.getOriginalFilename(), file.getSize(), file.getContentType());
                
                try {
                    // 上传图片并设置最大宽高为1024
                    String url = publicFileUploadService.uploadImage(file, userId, 1024, 1024);
                    if (url != null) {
                        mediaUrls.add(url);
                        log.info("图片上传成功: URL={}", url);
                    } else {
                        log.error("图片上传返回null URL");
                    }
                } catch (Exception e) {
                    log.error("上传单个图片时出错: 文件名={}", file.getOriginalFilename(), e);
                }
            }
            
            log.info("图片批量上传完成，成功上传{}张图片", mediaUrls.size());
            
            if (mediaUrls.isEmpty() && !files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "所有图片上传均失败"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("图片上传成功", mediaUrls));
        } catch (Exception e) {
            log.error("处理图片上传请求时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "图片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传视频文件
     *
     * @param file 视频文件
     * @param userDetails 当前用户
     * @return 视频URL
     */
    @PostMapping("/upload/video")
    public ResponseEntity<ApiResponse<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            log.info("用户 {} 上传视频, 文件名: {}, 大小: {}字节, 类型: {}", 
                userId, file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            if (userId == null) {
                log.error("上传视频失败: 用户ID为null，可能未通过认证");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
            
            if (file == null || file.isEmpty()) {
                log.error("上传视频失败: 文件为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "未提供有效的视频文件"));
            }
            
            // 检查文件类型
            if (!file.getContentType().startsWith("video/")) {
                log.error("上传视频失败: 无效的文件类型 {}", file.getContentType());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "仅支持上传视频文件"));
            }
            
            // 使用PublicFileUploadService而非FileUploadService上传视频，确保视频为公开可访问
            String videoUrl = publicFileUploadService.uploadFile(file, userId);
            
            if (videoUrl == null) {
                log.error("视频上传失败: 服务返回null");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(500, "视频上传处理失败"));
            }
            
            log.info("视频上传成功: URL={}", videoUrl);
            
            return ResponseEntity.ok(ApiResponse.success("视频上传成功", videoUrl));
        } catch (Exception e) {
            log.error("处理视频上传请求时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "视频上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取个人动态列表
     *
     * @param userId 用户ID（可选，默认为当前用户）
     * @param page 页码
     * @param size 每页大小
     * @param userDetails 当前用户
     * @return 动态列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<MomentResponse>>> getUserMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long currentUserId = extractUserId(userDetails);
            log.info("获取用户 {} 的动态列表, 请求者: {}, 页码: {}, 大小: {}", 
                userId, currentUserId, page, size);
            
            if (currentUserId == null) {
                log.error("获取动态列表失败: 用户ID为null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<MomentResponse> moments = momentService.getUserTimeline(userId, pageable);
            log.info("获取动态列表成功，条数: {}, 总页数: {}", 
                moments.getContent().size(), moments.getTotalPages());
            
            return ResponseEntity.ok(ApiResponse.success(moments));
        } catch (Exception e) {
            log.error("获取用户动态时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "获取动态列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取朋友圈动态列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param userDetails 当前用户
     * @return 动态列表
     */
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<Page<MomentResponse>>> getFriendTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            log.info("获取用户 {} 的朋友圈动态列表, 页码: {}, 大小: {}", 
                userId, page, size);
            
            if (userId == null) {
                log.error("获取朋友圈动态失败: 用户ID为null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录或会话已过期"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<MomentResponse> moments = momentService.getFriendTimeline(userId, pageable);
            log.info("获取朋友圈动态成功，条数: {}, 总页数: {}", 
                moments.getContent().size(), moments.getTotalPages());
            
            return ResponseEntity.ok(ApiResponse.success(moments));
        } catch (Exception e) {
            log.error("获取朋友圈动态时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "获取朋友圈动态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取动态详情
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 动态详情
     */
    @GetMapping("/{momentId}")
    public ResponseEntity<ApiResponse<MomentDetailResponse>> getMomentDetail(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("获取动态 {} 详情, 请求者: {}", momentId, userId);
        
        MomentDetailResponse moment = momentService.getMomentDetail(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success(moment));
    }

    /**
     * 更新动态内容
     *
     * @param momentId 动态ID
     * @param content 更新的内容
     * @param userDetails 当前用户
     * @return 更新后的动态
     */
    @PutMapping("/{momentId}")
    public ResponseEntity<ApiResponse<MomentResponse>> updateMoment(
            @PathVariable Long momentId,
            @RequestBody String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("更新动态 {}, 请求者: {}", momentId, userId);
        
        MomentResponse moment = momentService.updateMoment(momentId, userId, content);
        return ResponseEntity.ok(ApiResponse.success("动态更新成功", moment));
    }

    /**
     * 删除动态
     *
     * @param momentId 动态ID
     * @param userDetails 当前用户
     * @return 操作结果
     */
    @DeleteMapping("/{momentId}")
    public ResponseEntity<ApiResponse<Void>> deleteMoment(
            @PathVariable Long momentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("删除动态 {}, 请求者: {}", momentId, userId);
        
        boolean success = momentService.deleteMoment(momentId, userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("动态删除成功", null));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "无权删除该动态"));
        }
    }

    /**
     * 从UserDetails中提取用户ID
     * 如果UserDetails为null，则尝试从请求头或请求参数中获取用户ID
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
        
        log.warn("无法获取用户ID，认证信息可能缺失");
        return null;
    }
} 