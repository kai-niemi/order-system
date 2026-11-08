package se.cloudneutral.orders.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    DECLINED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
