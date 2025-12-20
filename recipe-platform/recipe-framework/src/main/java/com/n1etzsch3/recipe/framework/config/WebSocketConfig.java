package com.n1etzsch3.recipe.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册 WebSocket 端点
     * 注意：如果使用外部 Servlet 容器（如 Tomcat 部署 war 包），需要移除此 Bean
     * 因为外部容器会自己管理 WebSocket 端点
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
