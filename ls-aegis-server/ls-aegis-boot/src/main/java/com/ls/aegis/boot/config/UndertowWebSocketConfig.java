package com.ls.aegis.boot.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Undertow WebSocket ???? BufferPool??? UT026010 ?????
 * <p>?????????? buffers?????????? ContiNew ??????</p>
 */
@Configuration
public class UndertowWebSocketConfig {

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowWebSocketBufferPoolCustomizer() {
        return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            Map<String, Object> attrs = deploymentInfo.getServletContextAttributes();
            Object attr = attrs == null ? null : attrs.get(WebSocketDeploymentInfo.ATTRIBUTE_NAME);
            WebSocketDeploymentInfo info = attr instanceof WebSocketDeploymentInfo websocketInfo
                ? websocketInfo
                : new WebSocketDeploymentInfo();
            if (info.getBuffers() == null) {
                info.setBuffers(new DefaultByteBufferPool(true, 1024));
            }
            deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
        });
    }
}
