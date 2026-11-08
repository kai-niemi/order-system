package se.cloudneutral.orders.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ShipmentStatus {
    PLACED,
    CONFIRMED,
    PAYMENT_PENDING,
    PAID,
    CANCELLED,
    DELIVERED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
