package com.likelion.animalface.global.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableFeignClients(basePackages = "com.likelion.animalface")
@Configuration
public class FeginConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        // AI 서버와의 통신 로그를 상세히 보기 위해 FULL로 설정 (운영시 BASIC 권장)
        return Logger.Level.FULL;
    }

    @Bean
    public Retryer retryer() {
        // 통신 실패 시 재시도 간격 및 횟수 설정 (100ms 간격으로 시작, 최대 1초, 3번 시도)
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }
}
