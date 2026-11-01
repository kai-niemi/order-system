package se.cloudneutral.orders.api;

import java.net.URI;

/**
 * The RFC 9457 problem types of the API. Like the extension relations, a type is identified by a
 * URI that does not depend on where the server is deployed, so clients can switch on it rather
 * than on the status code or the detail text. Each type is documented at /problems/{type}.
 * Errors that mean no more than their status code are left with the default type, about:blank.
 */
public abstract class ProblemTypes {
    private ProblemTypes() {
    }

    public static final String TYPE_URI_TEMPLATE = "https://order-system.cloudneutral.se/problems/{type}";

    public static final URI IDEMPOTENCY_KEY_REQUIRED = of("idempotency-key-required");

    public static final URI IDEMPOTENCY_KEY_REUSED = of("idempotency-key-reused");

    public static final URI IDEMPOTENCY_RECORD_LOST = of("idempotency-record-lost");

    public static final URI UNKNOWN_REFERENCE = of("unknown-reference");

    public static final URI ORDER_REJECTED = of("order-rejected");

    public static final URI ORDER_NOT_CANCELLABLE = of("order-not-cancellable");

    private static URI of(String type) {
        return URI.create(TYPE_URI_TEMPLATE.replace("{type}", type));
    }
}
