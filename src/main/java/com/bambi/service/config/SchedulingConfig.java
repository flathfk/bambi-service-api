package com.bambi.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 활성화 (발행 폴링·Agent Context Outbox 재시도용).
 * 스케줄러 자체는 항상 켜두고 각 워커 빈은 대응하는 enabled 설정으로 제어한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
