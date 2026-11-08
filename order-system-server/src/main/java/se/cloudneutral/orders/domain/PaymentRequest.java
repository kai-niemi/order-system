package se.cloudneutral.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        boolean simulateTimeout
) {
}