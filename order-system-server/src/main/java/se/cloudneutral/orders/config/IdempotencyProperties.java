package se.cloudneutral.orders.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param retention     how long an idempotency record stays live after it is claimed
 * @param evictInterval delay between eviction runs, measured from the end of the previous run
 * @param evictBatch    maximum number of expired records deleted per transaction
 */
@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(
        Duration retention,
        Duration evictInterval,
        Integer evictBatch
) {
    public IdempotencyProperties {
        if (evictInterval == null) {
            evictInterval = Duration.ofMinutes(10);
        }
        if (evictBatch == null) {
            evictBatch = 1000;
        }
    }
}
