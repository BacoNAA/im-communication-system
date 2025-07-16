package com.im.imcommunicationsystem.auth.interceptor;

import com.im.imcommunicationsystem.auth.service.DeviceService;
import com.im.imcommunicationsystem.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 设备活跃度拦截器
 * 用于在用户进行API调用时更新设备的最后活跃时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceActivityInterceptor implements HandlerInterceptor {

    private final DeviceService deviceService;
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 获取Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // 验证token并获取用户信息
                if (jwtUtils.validateAccessToken(token)) {
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    String deviceType = jwtUtils.getDeviceTypeFromToken(token);
                    
                    if (userId != null && deviceType != null) {
                        // 更新设备最后活跃时间
                        deviceService.updateDeviceLastActiveTimeByType(userId, deviceType);
                    }
                }
            }
        } catch (Exception e) {
            // 记录错误但不影响请求处理
            log.debug("更新设备活跃时间时发生错误: {}", e.getMessage());
        }
        
        return true;
    }
}