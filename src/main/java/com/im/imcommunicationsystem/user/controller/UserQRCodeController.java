package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.user.dto.response.QRCodeResponse;
import com.im.imcommunicationsystem.user.service.UserQRCodeService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 二维码控制器
 * 处理二维码相关的HTTP请求
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserQRCodeController {

    private final UserQRCodeService userQRCodeService;
    private final SecurityUtils securityUtils;

    /**
     * 生成用户二维码名片
     * 
     * @return 二维码信息
     */
    @GetMapping("/qrcode")
    public ResponseEntity<ApiResponse<QRCodeResponse>> generateUserQRCode() {
        try {
            // 从JWT token中获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户未登录或登录已过期"));
            }
            
            QRCodeResponse response = userQRCodeService.generateUserQRCode(userId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
             return ResponseEntity.ok(ApiResponse.error(500, "生成二维码失败: " + e.getMessage()));
         }
    }

    /**
     * 解析二维码获取用户信息
     * 
     * @param file 二维码图片文件
     * @return 用户信息
     */
    @PostMapping("/qrcode/parse")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseQRCode(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "请上传二维码图片文件"));
            }
            
            Map<String, Object> userInfo = userQRCodeService.parseUserQRCode(file);
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
            
        } catch (Exception e) {
             return ResponseEntity.ok(ApiResponse.error(500, "解析二维码失败: " + e.getMessage()));
         }
    }
}