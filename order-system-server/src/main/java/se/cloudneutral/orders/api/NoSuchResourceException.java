package se.cloudneutral.orders.api;

import java.util.UUID;

/**
 * Raised when a request body references an entity id that does not exist. The request
 * itself is well-formed, hence this maps to 422 rather than 400 or 404.
 */
public class NoSuchResourceException extends RuntimeException {
    public static NoSuchResourceException customer(UUID id) {
        return new NoSuchResourceException("No such customer: %s".formatted(id));
    }

    public static NoSuchResourceException product(UUID id) {
        return new NoSuchResourceException("No such product: %s".formatted(id));
    }

    public NoSuchResourceException(String message) {
        super(message);
    }
}
