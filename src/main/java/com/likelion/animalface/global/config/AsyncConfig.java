package com.likelion.animalface.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        // Java 21의 가상 스레드 전용 Executor를 사용합니다.
        // 스레드 풀 개수 제한 없이 효율적으로 I/O를 처리합니다.
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}