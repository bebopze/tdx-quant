package com.bebopze.tdx.quant.task.progress;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author: bebopze
 * @date: 2025/9/15
 */
@Slf4j
@Component
public class TaskProgressWebSocketHandler extends TextWebSocketHandler {


    // 存储 session 和订阅的任务ID映射
    private final Map<WebSocketSession, String> sessionSubscriptions = new ConcurrentHashMap<>();
    private final TaskProgressManager taskProgressManager;


    @Autowired
    public TaskProgressWebSocketHandler(TaskProgressManager taskProgressManager) {
        this.taskProgressManager = taskProgressManager;
        // 设置反向引用
        this.taskProgressManager.setWebSocketHandler(this);
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("收到WebSocket消息: {} Session: {}", payload, session.getId());

            // 解析消息类型
            if (payload.startsWith("SUBSCRIBE:")) {
                // 订阅特定任务
                String taskId = payload.substring(10);
                sessionSubscriptions.put(session, taskId);
                sendTaskProgress(session, taskId);
            } else if ("GET_ACTIVE_TASKS".equals(payload)) {
                // 获取所有活跃任务
                List<TaskProgress> activeTasks = taskProgressManager.getAllActiveTasks();
                session.sendMessage(new TextMessage(JSON.toJSONString(activeTasks)));
            } else {
                // 默认认为是任务ID查询
                sendTaskProgress(session, payload);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            session.sendMessage(new TextMessage("{\"error\":\"处理消息失败: " + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket连接关闭: {} Status: {}", session.getId(), status);
        sessionSubscriptions.remove(session);
    }

    // 发送特定任务的进度
    private void sendTaskProgress(WebSocketSession session, String taskId) throws Exception {
        TaskProgress progress = taskProgressManager.getProgress(taskId);
        if (progress != null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(progress)));
        } else {
            session.sendMessage(new TextMessage("{\"error\":\"任务不存在: " + taskId + "\"}"));
        }
    }

    // 广播任务进度给所有订阅了该任务的客户端
    public void broadcastProgress(String taskId) {
        try {
            TaskProgress progress = taskProgressManager.getProgress(taskId);
            if (progress != null) {
                String json = JSON.toJSONString(progress);
                TextMessage message = new TextMessage(json);

                // 向订阅了此任务的所有客户端发送
                sessionSubscriptions.entrySet().stream()
                                    .filter(entry -> taskId.equals(entry.getValue()))
                                    .map(Map.Entry::getKey)
                                    .forEach(session -> {
                                        try {
                                            if (session.isOpen()) {
                                                session.sendMessage(message);
                                            }
                                        } catch (Exception e) {
                                            log.error("发送WebSocket消息失败", e);
                                            sessionSubscriptions.remove(session);
                                        }
                                    });
            }
        } catch (Exception e) {
            log.error("广播任务进度失败", e);
        }
    }


}