package se.cloudneutral.orders.service;

import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.domain.PaymentResult;

public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}
