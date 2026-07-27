package com.conclave.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import java.util.concurrent.Executors;

/**
 * Configuration class to enable and configure asynchronous execution using Java 21 Virtual Threads.
 */
@Configuration
public class AsyncConfig {

    /**
     * TaskExecutor bean configured to use Java 21 Virtual Threads.
     * Lightweight virtual threads are spawned per task, bypassing heavy OS thread pools.
     *
     * @return The AsyncTaskExecutor instance
     */
    @Bean(name = "conclaveTaskExecutor")
    public AsyncTaskExecutor conclaveTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
