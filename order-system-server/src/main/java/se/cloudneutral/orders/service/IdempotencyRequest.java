package se.cloudneutral.orders.service;

/**
 * Identifies an idempotent request.
 *
 * @param scope  the authenticated principal or tenant that owns the key, so that keys chosen by
 *               different clients never collide
 * @param key    the client supplied idempotency key
 * @param method the HTTP method, part of the request fingerprint
 * @param path   the request path, part of the request fingerprint
 * @param body   the request payload, fingerprinted in canonical JSON form
 */
public record IdempotencyRequest(
        String scope,
        String key,
        String method,
        String path,
        Object body
) {
}
