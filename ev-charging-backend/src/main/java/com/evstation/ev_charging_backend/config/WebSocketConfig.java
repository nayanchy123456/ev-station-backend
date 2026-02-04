package com.evstation.ev_charging_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${spring.websocket.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${spring.websocket.heartbeat.server:25000}")
    private long serverHeartbeat;

    @Value("${spring.websocket.heartbeat.client:25000}")
    private long clientHeartbeat;

    @Value("${spring.websocket.message.size.limit:524288}")
    private int messageSizeLimit;

    @Value("${spring.websocket.buffer.size.limit:1048576}")
    private int bufferSizeLimit;

    @Value("${spring.websocket.send.time.limit:20000}")
    private long sendTimeLimit;

    // ================= MESSAGE BROKER =================

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{serverHeartbeat, clientHeartbeat})
                .setTaskScheduler(heartbeatTaskScheduler());

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");

        log.info("WebSocket Message Broker initialized");
    }

    // ================= STOMP ENDPOINTS =================

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(serverHeartbeat)
                .setDisconnectDelay(5000);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        log.info("STOMP WebSocket endpoints registered");
    }

    // ================= CHANNEL CONFIG =================

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(webSocketAuthInterceptor)
                .taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(1000);

        log.info("Inbound WebSocket channel configured");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {

        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(1000);

        log.info("Outbound WebSocket channel configured");
    }

    // ================= TRANSPORT =================

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {

        registration
                .setMessageSizeLimit(messageSizeLimit)
                .setSendBufferSizeLimit(bufferSizeLimit)
                .setSendTimeLimit((int) sendTimeLimit)
                .setTimeToFirstMessage(30000);

        log.info("WebSocket transport tuned");
    }

    // ================= HEARTBEAT SCHEDULER =================

    @Bean
    public TaskScheduler heartbeatTaskScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();

        return scheduler;
    }

    // ================= SERVLET CONTAINER =================

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {

        ServletServerContainerFactoryBean container =
                new ServletServerContainerFactoryBean();

        container.setMaxTextMessageBufferSize(bufferSizeLimit);
        container.setMaxBinaryMessageBufferSize(bufferSizeLimit);
        container.setMaxSessionIdleTimeout(300000L);   // 5 minutes
        container.setAsyncSendTimeout(sendTimeLimit); // FIXED

        log.info("Servlet WebSocket container configured");

        return container;
    }
}
