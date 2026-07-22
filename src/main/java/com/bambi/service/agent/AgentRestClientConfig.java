package com.bambi.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * agent-api 호출용 {@link RestClient} 빈. base-url·타임아웃을 여기서 고정한다.
 * (Gateway 에서 직접 만들지 않고 주입받아 테스트에서 MockRestServiceServer 로 갈아끼울 수 있게 분리)
 */
@Configuration
public class AgentRestClientConfig {

    @Bean
    public RestClient agentRestClient(
            @Value("${app.agent.base-url}") String baseUrl,
            @Value("${app.agent.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.agent.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
