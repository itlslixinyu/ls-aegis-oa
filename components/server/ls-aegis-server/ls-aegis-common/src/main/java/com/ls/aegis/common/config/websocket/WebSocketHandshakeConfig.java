package com.ls.aegis.common.config.websocket;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import top.continew.starter.messaging.websocket.autoconfigure.WebSocketProperties;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;
import top.continew.starter.messaging.websocket.core.WebSocketInterceptor;

import java.util.Map;

/**
 * WebSocket 握手：通过 Sec-WebSocket-Protocol 传递 Token，避免 query 参数进入访问日志。
 */
@Configuration
public class WebSocketHandshakeConfig {

    @Bean
    @Primary
    public HandshakeInterceptor handshakeInterceptor(WebSocketProperties properties,
        WebSocketClientService webSocketClientService) {
        WebSocketInterceptor delegate = new WebSocketInterceptor(properties, webSocketClientService);
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) {
                echoSubprotocol(request, response);
                return delegate.beforeHandshake(request, response, wsHandler, attributes);
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
                delegate.afterHandshake(request, response, wsHandler, exception);
            }
        };
    }

    private static void echoSubprotocol(ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return;
        }
        String protocol = servletRequest.getServletRequest().getHeader("Sec-WebSocket-Protocol");
        if (StrUtil.isBlank(protocol)) {
            return;
        }
        String token = protocol.contains(",") ? protocol.substring(0, protocol.indexOf(',')).trim() : protocol.trim();
        if (StrUtil.isNotBlank(token)) {
            response.getHeaders().set("Sec-WebSocket-Protocol", token);
        }
    }
}
