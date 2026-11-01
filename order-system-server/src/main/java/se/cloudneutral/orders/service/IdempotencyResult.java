package se.cloudneutral.orders.service;

public record IdempotencyResult<T>(
        T resource,
        boolean replayed
) {
}