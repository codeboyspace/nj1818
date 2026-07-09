package com.genc.arfoms.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Provides the HTTP clients the gateway uses to forward requests to the module
 * services.
 *
 * <p>This is a single-machine dev setup: the gateway targets each module by its
 * fixed absolute {@code http://localhost:<port>} base URL (see
 * {@code gateway.*.base-url} in application.properties), so these clients must
 * NOT be {@code @LoadBalanced}. A load-balanced client treats the URL host
 * (e.g. {@code localhost}) as a Spring Cloud service id and tries to resolve it
 * against Eureka, which fails with "No instances available for localhost" and
 * surfaces as an HTTP 500 from the gateway. Leaving the clients plain also keeps
 * the Eureka client's own transport non-load-balanced, so the gateway can
 * register with the registry at {@code http://localhost:8761/eureka/}.
 */
@Configuration
public class LoadBalancerConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Plain {@link RestClient.Builder}. Controllers build their {@link RestClient}
     * from this builder using absolute {@code http://localhost:<port>} base URLs.
     */
    @Bean
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}

