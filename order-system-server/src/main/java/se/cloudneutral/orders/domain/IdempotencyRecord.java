package se.cloudneutral.orders.domain;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        String scope,
        String idempotencyKey,
        byte[] fingerprint,
        UUID resourceId,
        Instant createdAt,
        Instant expiresAt
) {
}
