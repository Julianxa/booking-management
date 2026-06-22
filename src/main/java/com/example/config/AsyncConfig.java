package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reminderExecutor")
    public Executor reminderExecutor(
            @Value("${app.reminder.executor.core-pool-size:4}") int corePoolSize,
            @Value("${app.reminder.executor.max-pool-size:16}") int maxPoolSize,
            @Value("${app.reminder.executor.queue-capacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("reminder-");
        executor.initialize();
        return executor;
    }
}
