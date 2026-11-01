package se.cloudneutral.orders.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.config.IdempotencyProperties;
import se.cloudneutral.orders.repository.IdempotencyRepository;

/**
 * Physically removes expired idempotency records using plain SQL, so it does not depend on any
 * database specific TTL feature. Expired rows are already treated as absent by the claim, so
 * eviction only reclaims space and its lag does not affect the replay window.
 * <p>
 * Safe to run on every instance: each batch is its own short transaction that locks rows with
 * {@code skip locked}, so instances take disjoint batches and no leader election is needed.
 */
@Component
public class IdempotencyEvictor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IdempotencyRepository repository;

    @Autowired
    private IdempotencyProperties properties;

    @Scheduled(fixedDelayString = "${app.idempotency.evict-interval:PT10M}")
    @TransactionBoundary
    public void evictExpired() {
        int batchSize = properties.evictBatch();
        long total = 0;

        try {
            int deleted;

            do {
                deleted = repository.deleteExpired(batchSize);
                total += deleted;
            } while (deleted >= batchSize);

            if (total > 0) {
                logger.info("Evicted {} expired idempotency records", total);
            }
        } catch (DataAccessException e) {
            // Retried on the next run, nothing is lost by a failed batch
            logger.warn("Idempotency eviction failed after deleting {} records", total, e);
        }
    }
}
