package se.cloudneutral.orders.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.api.ResourceNotFoundException;
import se.cloudneutral.orders.domain.InvalidStateTransitionException;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;
import static se.cloudneutral.orders.domain.ShipmentStatus.PAYMENT_PENDING;

@Service
public class DefaultPaymentService implements PaymentService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Override
    @TransactionBoundary
    public void initiatePayment(UUID orderId, UUID paymentAttemptId, boolean simulateTimeout) {
        // Lock the order row so the status check and the transition to PAYMENT_PENDING are atomic.
        // Concurrent payments (with different idempotency keys) and cancellations serialize here,
        // and the loser observes the new status instead of acting on a stale PLACED.
        PurchaseOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        if (order.getStatus() != ShipmentStatus.PLACED) {
            throw new InvalidStateTransitionException(
                    "Order %s cannot be paid in state %s, expected PLACED"
                            .formatted(orderId, order.getStatus()));
        }

        PaymentAttempt payment = PaymentAttempt.pending(
                paymentAttemptId,
                orderId,
                order.getTotalPrice(),
                simulateTimeout
        );

        order.setStatus(PAYMENT_PENDING);

        paymentAttemptRepository.save(payment);
    }

    @Override
    public Optional<PaymentAttempt> getPaymentAttempt(UUID paymentAttemptId) {
        return paymentAttemptRepository.findById(paymentAttemptId);
    }
}
