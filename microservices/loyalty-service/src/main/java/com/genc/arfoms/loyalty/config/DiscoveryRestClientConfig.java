package com.genc.arfoms.loyalty.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Exposes a load-balanced {@link RestClient.Builder} so the loyalty service can
 * reach other services via {@code http://<service-id>} URLs resolved through
 * Eureka. The {@code @Primary} plain builder is kept for infrastructure use.
 */
@Configuration
public class DiscoveryRestClientConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}

