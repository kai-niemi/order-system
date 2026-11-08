package se.cloudneutral.orders.domain;

/**
 * Raised when an operation is not legal in the current state of an order, for example paying a
 * cancelled order. The request is well-formed but conflicts with the state of the resource, hence
 * this maps to 409.
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
