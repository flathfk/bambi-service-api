package com.bambi.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 활성화 (service-worker 발행 폴링 루프용).
 * 스케줄러 자체는 항상 켜두고, 워커 빈은 app.worker.publish.enabled 로 제어한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
