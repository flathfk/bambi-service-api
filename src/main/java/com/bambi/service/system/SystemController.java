package com.bambi.service.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Blue-Green 무중단 배포에 필요한 헬스체크/버전 엔드포인트.
 * CLAUDE.md의 필수 API: GET /api/health, GET /api/version
 */
@RestController
@RequestMapping("/api")
public class SystemController {

    @Value("${app.version:0.0.1}")
    private String version;

    @Value("${app.color:blue}")
    private String color;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of(
                "version", version,
                "color", color
        );
    }
}
