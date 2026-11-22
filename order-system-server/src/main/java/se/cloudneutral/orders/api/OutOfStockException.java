package se.cloudneutral.orders.api;

import java.util.UUID;

public class OutOfStockException extends RuntimeException {
    public static OutOfStockException product(UUID id, int current, int quantity) {
        return new OutOfStockException("Not enough items in stock: %s (current=%d, qty=%d)"
                .formatted(id, current, quantity));
    }

    public OutOfStockException(String message) {
        super(message);
    }
}

