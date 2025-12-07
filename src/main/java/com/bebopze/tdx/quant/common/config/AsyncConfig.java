package com.bebopze.tdx.quant.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;


/**
 * 异步request  ->  获取当前线程的请求上下文
 *
 * @author: bebopze
 * @date: 2025/12/8
 */
@Configuration
@EnableAsync
public class AsyncConfig {


    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("task-");
        // 设置 TaskDecorator 以复制上下文
        executor.setTaskDecorator(new ContextCopyingTaskDecorator());
        executor.initialize();
        return executor;
    }


    static class ContextCopyingTaskDecorator implements TaskDecorator {


        @Override
        public Runnable decorate(Runnable runnable) {

            // 在装饰时（即提交任务时），获取当前线程的请求上下文
            RequestAttributes context = RequestContextHolder.currentRequestAttributes();

            return () -> {
                try {
                    // 在新线程中设置获取到的请求上下文
                    RequestContextHolder.setRequestAttributes(context);
                    // 执行原任务
                    runnable.run();
                } finally {
                    // 清理，防止内存泄漏
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        }
    }


}