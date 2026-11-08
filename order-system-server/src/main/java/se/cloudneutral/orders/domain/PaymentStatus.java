package se.cloudneutral.orders.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    DECLINED,
    /**
     * The provider outcome is unknown after the retries were exhausted. The money may or may not
     * have been taken, so the attempt stays until a reconciliation yields a definitive result.
     */
    UNRESOLVED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
