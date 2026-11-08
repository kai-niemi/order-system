package se.cloudneutral.orders.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PaymentResult;
import se.cloudneutral.orders.domain.PaymentStatus;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;

@Service
public class PaymentCompletionService {
    private final PaymentAttemptRepository paymentRepository;

    private final OrderRepository orderRepository;

    public PaymentCompletionService(PaymentAttemptRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @TransactionBoundary
    public void complete(UUID paymentId, PaymentResult result) {
        PaymentAttempt attempt = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment attempt not found: " + paymentId)
                );

        // A completed payment must not be applied twice.
        if (attempt.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        PurchaseOrder order = orderRepository
                .findByIdForUpdate(attempt.getOrderId())
                .orElseThrow();

        attempt.complete(result);

        switch (result.status()) {
            case SUCCEEDED -> order.paymentSucceeded();
            case DECLINED -> order.paymentDeclined();
        }
    }
}