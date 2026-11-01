package se.cloudneutral.orders.service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
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
     * <p>
     * The id of the resource to create is minted up front and written to the idempotency record
     * in the same statement that claims the key, so a record never exists without a resource.
     * The record and the resource commit or roll back together.
     *
     * @param request        identifies the request and carries the payload to fingerprint
     * @param operation      creates the resource using the supplied, pre-generated id
     * @param representation builds the response from a resource id
     */
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

        // A live record exists, so replay or reject. Absence here is not expected: the
        // conflicting row was just observed within this same serializable transaction. It is
        // kept as a safety net for a real anomaly rather than as a client-facing scenario.
        IdempotencyRecord existing = repository.findByKey(request.scope(), request.key())
                .orElseThrow(() -> new IdempotencyException(
                        IdempotencyException.Reason.RECORD_LOST,
                        "Idempotency record lost for key: " + request.key()));

        if (!MessageDigest.isEqual(existing.fingerprint(), fingerprint)) {
            throw new IdempotencyException(
                    IdempotencyException.Reason.FINGERPRINT_MISMATCH,
                    "Idempotency key was previously used with a different request");
        }

        return new IdempotencyResult<>(representation.apply(existing.resourceId()), true);
    }
}
