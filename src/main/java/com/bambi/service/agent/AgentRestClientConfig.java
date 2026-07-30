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
 */
@Configuration
public class AgentRestClientConfig {

    @Bean
    public RestClient agentRestClient(
            @Value("${app.agent.base-url}") String baseUrl,
            @Value("${app.agent.internal-token:}") String internalToken,
            @Value("${app.agent.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.agent.read-timeout-ms}") int readTimeoutMs) {
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
