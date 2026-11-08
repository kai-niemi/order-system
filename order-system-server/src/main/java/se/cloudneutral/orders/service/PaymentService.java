package se.cloudneutral.orders.service;

import java.util.Optional;
import java.util.UUID;

import se.cloudneutral.orders.domain.PaymentAttempt;

public interface PaymentService {
    void initiatePayment(UUID orderId, UUID paymentAttemptId, boolean simulateTimeout);

    Optional<PaymentAttempt> getPaymentAttempt(UUID paymentAttemptId);
}
