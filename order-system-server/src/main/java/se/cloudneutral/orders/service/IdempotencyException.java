package se.cloudneutral.orders.service;

public class IdempotencyException extends RuntimeException {
    public enum Reason {
        FINGERPRINT_MISMATCH,
        RECORD_LOST
    }

    private final Reason reason;

    public IdempotencyException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}