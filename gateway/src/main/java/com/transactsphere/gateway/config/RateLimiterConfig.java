package com.transactsphere.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the user's remote IP address to use as the rate limiting key.
     * If the remote address is null, defaults to "anonymous".
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? Objects.requireNonNull(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())
                        : "anonymous"
        );
    }
}
