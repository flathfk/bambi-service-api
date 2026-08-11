package com.bambi.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * agent-api 호출용 {@link RestClient} 빈. base-url·타임아웃·내부 인증을 여기서 고정한다.
 * (Gateway 에서 직접 만들지 않고 주입받아 테스트에서 MockRestServiceServer 로 갈아끼울 수 있게 분리)
 *
 * 아침 브리핑 주제 조회도 REPORT-022부터 DB Snapshot만 읽으므로 모든 동기 호출에 같은
 * 짧은 타임아웃을 적용한다.
 */
@Configuration
public class AgentRestClientConfig {

    /** 가벼운 동기 호출용(컨텍스트 동기화·위키 조회 등). 짧게 끊는다. */
    @Bean
    public RestClient agentRestClient(
            @Value("${app.agent.base-url}") String baseUrl,
            @Value("${app.agent.internal-token:}") String internalToken,
            @Value("${app.agent.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.agent.read-timeout-ms}") int readTimeoutMs) {
        return build(baseUrl, internalToken, connectTimeoutMs, readTimeoutMs);
    }

    private RestClient build(String baseUrl, String internalToken,
                             int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);
        // agent 는 토큰 미설정 시 503, 불일치 시 401 을 반환한다(2026-07-30 도입).
        // 값이 비어 있으면 헤더를 붙이지 않아 무인증 로컬 agent 와도 호환된다.
        if (!internalToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken);
        }
        return builder.build();
    }
}
