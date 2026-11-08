package se.cloudneutral.orders.api;

import java.util.UUID;

/**
 * Raised when the resource addressed by the request path does not exist, hence 404. Contrast
 * with {@link NoSuchResourceException}, which is for ids referenced from the request body.
 */
public class ResourceNotFoundException extends RuntimeException {
    public static ResourceNotFoundException order(UUID id) {
        return new ResourceNotFoundException("No such order: %s".formatted(id));
    }

    public static ResourceNotFoundException payment(UUID id) {
        return new ResourceNotFoundException("No such payment attempt: %s".formatted(id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
