package se.cloudneutral.orders.service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.aspect.ExceptionClassifier;
import se.cloudneutral.orders.config.IdempotencyProperties;
import se.cloudneutral.orders.domain.IdempotencyRecord;
import se.cloudneutral.orders.repository.IdempotencyRepository;
import se.cloudneutral.orders.util.DigestUtils;

@Service
public class IdempotencyService {
    @Autowired
    private IdempotencyRepository repository;

    @Autowired
    private IdempotencyProperties properties;

    /**
     * Executes an operation at most once per (scope, key) within the retention period.
     *
     * <p>
     * The resource identifier is generated before key acquisition and stored in the
     * idempotency record. Key acquisition and resource creation execute within the
     * same database transaction, ensuring that neither can commit independently.
     *
     * <p>
     * Supports both READ COMMITTED and SERIALIZABLE isolation. Concurrent requests
     * using the same key are coordinated through the database's unique constraint.
     * Under READ COMMITTED, a waiting request can observe the committed record and
     * replay the existing resource, or acquire the key if the competing transaction
     * rolls back.
     *
     * <p>
     * Under SERIALIZABLE, a competing request may encounter a serialization failure
     * if the conflicting record was committed after its transaction snapshot was
     * established. The retry interceptor retries the entire transaction using a new
     * snapshot. The retry must execute outside the transaction boundary; see
     * AdvisorOrder.
     *
     * @param request        identifies the request and carries the payload to fingerprint
     * @param operation      creates the resource using the supplied, pre-generated ID
     * @param representation builds the response from a resource ID
     */
    // Important: Retry advice must wrap transaction advice so each attempt start a new transaction. See AdvisorOrder.
    @Retryable(predicate = ExceptionClassifier.class,
            maxRetries = 5, delay = 25, multiplier = 2, maxDelay = 500, jitter = 10)
    @TransactionBoundary
    public <T> IdempotencyResult<T> execute(
            IdempotencyRequest request,
            Consumer<UUID> operation,
            Function<UUID, T> representation) {

        byte[] fingerprint = DigestUtils.toCanonicalHash(
                request.method(), request.path(), request.body());

        Optional<UUID> claimed = repository.acquire(
                request.scope(),
                request.key(),
                fingerprint,
                UUID.randomUUID(),
                Instant.now().plus(properties.retention())
        );

        if (claimed.isPresent()) {
            UUID resourceId = claimed.get();
            operation.accept(resourceId);
            return new IdempotencyResult<>(representation.apply(resourceId), false);
        }

        // An unsuccessful acquisition implies an existing, unexpired record.
        // ON CONFLICT DO UPDATE locks the conflicting row until transaction end,
        // even when its WHERE condition is false. Concurrent deletion is therefore
        // prevented until the lookup completes.
        IdempotencyRecord existing = repository.findByKey(request.scope(), request.key())
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency record lost for key: " + request.key()));

        if (!MessageDigest.isEqual(existing.fingerprint(), fingerprint)) {
            throw new IdempotencyException(
                    IdempotencyException.Reason.FINGERPRINT_MISMATCH,
                    "Idempotency key was previously used with a different request");
        }

        return new IdempotencyResult<>(representation.apply(existing.resourceId()), true);
    }
}
