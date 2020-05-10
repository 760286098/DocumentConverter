package com.converter.config;

import com.converter.core.ConvertManager;
import com.converter.log.LogQueue;
import com.converter.pojo.LoggerMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.annotation.PostConstruct;

/**
 * WebSocket配置
 *
 * @author Evan
 */
@DependsOn("convertManager")
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 自动注入SimpMessagingTemplate
     *
     * @param messagingTemplate SimpMessagingTemplate对应的bean
     */
    @Autowired
    public void init(final @Qualifier("brokerMessagingTemplate") SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 注册
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    /**
     * 推送日志
     */
    @SuppressWarnings("InfiniteLoopStatement")
    @PostConstruct
    public void pushLogger() {
        ConvertManager.getThreadPoolTaskScheduler()
                .getScheduledThreadPoolExecutor()
                .execute(() -> {
                    while (true) {
                        try {
                            // 如果队列为空会阻塞
                            LoggerMessage message = LogQueue.getInstance().poll();
                            messagingTemplate.convertAndSend("/Logger", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}