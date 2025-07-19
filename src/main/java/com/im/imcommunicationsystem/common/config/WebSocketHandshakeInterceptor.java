package com.im.imcommunicationsystem.common.config;

import com.im.imcommunicationsystem.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket握手拦截器
 * 用于在WebSocket连接建立前进行身份验证
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @Value("${app.websocket.auth.required:true}")
    private boolean authRequired;

    /**
     * 在握手前执行，进行身份验证
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("WebSocket握手开始，请求URI: {}", request.getURI());
        
        // 记录所有请求头和参数，用于调试
        logRequestDetails(request);
        
        // 从请求中获取token
        String token = extractToken(request);
        
        // 如果在开发环境且未配置为必须验证，允许无token连接（用于测试）
        boolean isDev = "dev".equalsIgnoreCase(activeProfile) || "development".equalsIgnoreCase(activeProfile);
        
        if (!StringUtils.hasText(token)) {
            if (isDev && !authRequired) {
                // 开发环境下允许无token连接，设置测试用户ID
                log.warn("开发环境: 允许无token的WebSocket连接，使用测试用户ID");
                attributes.put("userId", 1L); // 使用ID为1的测试用户
                attributes.put("isTestUser", true);
                return true;
            } else {
                log.warn("WebSocket连接请求中未找到token，URI: {}", request.getURI());
                return false;
            }
        } else {
            log.info("WebSocket连接请求中找到token: {}", maskToken(token));
        }
        
        try {
            // 验证token并获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("从token中获取用户ID失败");
                return false;
            }
            
            // 将用户ID存储在WebSocket会话属性中
            attributes.put("userId", userId);
            log.info("WebSocket握手成功，用户ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket握手认证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 在握手完成后执行
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理逻辑
        if (exception != null) {
            log.error("WebSocket握手异常: {}", exception.getMessage());
        } else {
            log.info("WebSocket握手完成: {}", request.getURI());
        }
    }
    
    /**
     * 从请求中提取token
     */
    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 尝试从请求参数中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                log.debug("从请求参数中获取到token");
                return token;
            }
            
            // 尝试从请求头中获取token
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                log.debug("从请求头Authorization中获取到token");
                return authHeader.substring(7);
            }
            
            // 尝试从Sec-WebSocket-Protocol头获取token
            // 有些客户端会使用此头来传递认证信息
            String wsProtocol = servletRequest.getServletRequest().getHeader("Sec-WebSocket-Protocol");
            if (StringUtils.hasText(wsProtocol) && wsProtocol.startsWith("Bearer.")) {
                log.debug("从Sec-WebSocket-Protocol头中获取到token");
                return wsProtocol.substring(7);
            }
        }
        
        log.warn("无法从请求中提取token");
        return null;
    }
    
    /**
     * 记录请求的所有详细信息，用于调试
     */
    private void logRequestDetails(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest && "dev".equalsIgnoreCase(activeProfile)) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 记录所有请求头
            log.debug("WebSocket请求头信息:");
            Enumeration<String> headerNames = servletRequest.getServletRequest().getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    log.debug("  {} = {}", headerName, servletRequest.getServletRequest().getHeader(headerName));
                }
            }
            
            // 记录所有请求参数
            log.debug("WebSocket请求参数信息:");
            Map<String, String[]> paramMap = servletRequest.getServletRequest().getParameterMap();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (String value : entry.getValue()) {
                    if (sb.length() > 0) sb.append(", ");
                    
                    // 如果是token参数，则掩码显示
                    if ("token".equals(entry.getKey())) {
                        sb.append(maskToken(value));
                    } else {
                        sb.append(value);
                    }
                }
                log.debug("  {} = {}", entry.getKey(), sb.toString());
            }
        }
    }
    
    /**
     * 掩码显示token，只显示前10个字符
     */
    private String maskToken(String token) {
        if (token == null) return null;
        if (token.length() <= 10) return token.substring(0, 3) + "...";
        return token.substring(0, 10) + "...";
    }
} 