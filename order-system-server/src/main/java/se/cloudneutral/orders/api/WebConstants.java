package se.cloudneutral.orders.api;

public abstract class WebConstants {
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String HEADER_IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";

    public static final String HEADER_SIMULATE_TIMEOUT = "X-Simulate-Timeout";

    /**
     * Idempotency scope used when the request carries no authenticated principal.
     */
    public static final String ANONYMOUS_SCOPE = "anonymous";

    private WebConstants() {
    }
}
