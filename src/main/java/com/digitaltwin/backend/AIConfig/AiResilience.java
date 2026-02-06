package com.digitaltwin.backend.AIConfig;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AiResilience {

    private final CircuitBreaker cb;
    private final Bulkhead bulkhead;
    private final RateLimiter rateLimiter;

    public AiResilience(
            CircuitBreakerRegistry cbRegistry,
            BulkheadRegistry bulkheadRegistry,
            RateLimiterRegistry rlRegistry
    ) {
        this.cb = cbRegistry.circuitBreaker("ai");
        this.bulkhead = bulkheadRegistry.bulkhead("ai");
        this.rateLimiter = rlRegistry.rateLimiter("ai");
    }
}
