package com.im.imcommunicationsystem.common.config;

import com.im.imcommunicationsystem.common.service.impl.WebSocketServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket配置类
 * 配置WebSocket连接和处理器
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    @Autowired
    private WebSocketServiceImpl webSocketService;
    
    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    
    @Value("${app.websocket.message.max-text-message-size:64000}")
    private int maxTextMessageSize;
    
    @Value("${app.websocket.message.max-binary-message-size:1048576}")
    private int maxBinaryMessageSize;
    
    @Value("${app.websocket.message.buffer-size:8192}")
    private int bufferSize;

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
    }
    
    /**
     * 配置WebSocket传输选项
     * 
     * @param registration WebSocket传输注册器
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(maxTextMessageSize) // 设置消息大小限制（字节）
                    .setSendBufferSizeLimit(bufferSize) // 设置发送缓冲区大小限制（字节）
                    .setSendTimeLimit(20000); // 设置发送超时（毫秒）
    }
    
    /**
     * 注册WebSocket处理器
     * 配置原生WebSocket端点和处理器
     * 
     * @param registry WebSocket处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加原生WebSocket处理器，支持直接的WebSocket连接
        registry.addHandler(webSocketService, "/ws-native")
                .setAllowedOrigins("*")  // 允许所有来源
                .addInterceptors(webSocketHandshakeInterceptor);
    }
}