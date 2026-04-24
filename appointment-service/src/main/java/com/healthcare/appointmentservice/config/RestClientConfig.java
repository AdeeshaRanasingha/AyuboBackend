package com.healthcare.appointmentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean(name = "authRestClient")
    public RestClient authRestClient(@Value("${services.auth.base-url:http://localhost:8090}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean(name = "authFallbackRestClient")
    public RestClient authFallbackRestClient(
            @Value("${services.auth.fallback-base-url:http://host.docker.internal:8090}") String baseUrl
    ) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
