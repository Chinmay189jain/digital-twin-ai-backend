package com.digitaltwin.backend.AIConfig;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeoutException;

@Component
public class AiFailureMapper {

    private static final Logger logger = LoggerFactory.getLogger(AiFailureMapper.class);

    public AiFailure toFailure(Throwable ex) {
        Throwable root = unwrap(ex);

        logger.error("AiFailureMapper called", ex);

        if (root instanceof CallNotPermittedException) {
            return new AiFailure(AiErrorCode.AI_CB_OPEN,
                    "AI is temporarily overloaded. Please try again in a few seconds.");
        }

        if (root instanceof BulkheadFullException) {
            return new AiFailure(AiErrorCode.AI_BULKHEAD_FULL,
                    "Too many AI requests at once. Please try again shortly.");
        }

        if (root instanceof RequestNotPermitted) {
            return new AiFailure(AiErrorCode.AI_RATE_LIMIT,
                    "AI rate limit reached. Please retry after some time.");
        }

        if (root instanceof TimeoutException) {
            return new AiFailure(AiErrorCode.AI_TIMEOUT,
                    "AI is taking too long right now. Please retry.");
        }

        return new AiFailure(AiErrorCode.AI_UNKNOWN,
                "AI is temporarily unavailable. Please retry.");
    }

    private Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur != cur.getCause()) cur = cur.getCause();
        return cur;
    }
}
