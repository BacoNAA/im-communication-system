package com.im.imcommunicationsystem.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 配置STOMP协议的WebSocket消息代理
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * 设置消息订阅和发布的路径前缀
     * 
     * @param config 消息代理注册器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，并设置消息代理的目的地前缀
        // 客户端订阅地址的前缀信息
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // 客户端发送消息的地址的前缀信息
        config.setApplicationDestinationPrefixes("/app");
        
        // 点对点消息的前缀 无需配置 默认就是/user
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     * 设置WebSocket连接的端点
     * 
     * @param registry STOMP端点注册器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP协议的节点，并指定使用SockJS协议
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS(); // 支持SockJS
        
        // 注册WebSocket原生端点（不使用SockJS）
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    /**
     * WebSocket消息路由说明：
     * 
     * 1. 客户端连接端点：
     *    - SockJS: /ws
     *    - 原生WebSocket: /ws-native
     * 
     * 2. 消息发送路径（客户端 -> 服务器）：
     *    - 发送私聊消息: /app/chat/private
     *    - 发送群聊消息: /app/chat/group
     *    - 发送系统消息: /app/system/message
     *    - 用户上线通知: /app/user/online
     *    - 用户下线通知: /app/user/offline
     * 
     * 3. 消息订阅路径（服务器 -> 客户端）：
     *    - 私聊消息: /user/{userId}/queue/messages
     *    - 群聊消息: /topic/group/{groupId}
     *    - 系统通知: /topic/system/notifications
     *    - 用户状态: /topic/user/status
     *    - 在线用户列表: /topic/users/online
     * 
     * 4. 消息类型定义：
     *    - TEXT: 文本消息
     *    - IMAGE: 图片消息
     *    - VIDEO: 视频消息
     *    - FILE: 文件消息
     *    - AUDIO: 语音消息
     *    - SYSTEM: 系统消息
     *    - NOTIFICATION: 通知消息
     * 
     * 5. 连接认证：
     *    - 连接时需要在请求头中携带JWT token
     *    - 服务器验证token有效性后建立连接
     *    - 无效token将拒绝连接
     * 
     * 6. 心跳机制：
     *    - 客户端每30秒发送心跳包
     *    - 服务器超时60秒未收到心跳则断开连接
     *    - 支持断线重连机制
     */

}