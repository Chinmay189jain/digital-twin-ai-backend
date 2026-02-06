package com.digitaltwin.backend.AIConfig;

import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AiStreamGuard {

    private final AiResilience aiResilience;

    //  Wraps a streaming Flux with timeout + rate limit + bulkhead + circuit breaker
    public Flux<String> protect(Flux<String> rawStream) {
        return rawStream
                .timeout(Duration.ofSeconds(60))
                .transformDeferred(RateLimiterOperator.of(aiResilience.getRateLimiter()))
                .transformDeferred(BulkheadOperator.of(aiResilience.getBulkhead()))
                .transformDeferred(CircuitBreakerOperator.of(aiResilience.getCb()));
    }
}
