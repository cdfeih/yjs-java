package com.cdfeih.yjs.java.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类，用于支持实时通信和CRDT操作同步
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端将使用这个端点连接到服务器
        registry.addEndpoint("/yjs-websocket")
                .setAllowedOrigins("*")
                .withSockJS(); // 启用SockJS协议，提供降级选项
    }

    @Override
    public void configureMessageBroker(org.springframework.messaging.simp.config.MessageBrokerRegistry registry) {
        // 配置消息代理，用于将消息广播给客户端
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

}