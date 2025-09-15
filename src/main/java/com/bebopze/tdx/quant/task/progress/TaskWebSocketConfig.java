package com.bebopze.tdx.quant.task.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


/**
 * @author: bebopze
 * @date: 2025/9/15
 */
@Configuration
@EnableWebSocket
public class TaskWebSocketConfig implements WebSocketConfigurer {


    @Autowired
    private TaskProgressManager taskProgressManager;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TaskProgressWebSocketHandler(taskProgressManager), "/ws/task-progress")
                .setAllowedOrigins("*");
    }


}