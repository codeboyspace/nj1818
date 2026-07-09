package com.genc.arfoms.crew.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Exposes a load-balanced {@link RestClient.Builder} so inter-service clients
 * can target {@code http://<service-id>} URLs resolved through Eureka. A
 * {@code @Primary} plain builder is kept for infrastructure (e.g. the Eureka
 * client transport calling {@code http://localhost:8761/eureka}).
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


