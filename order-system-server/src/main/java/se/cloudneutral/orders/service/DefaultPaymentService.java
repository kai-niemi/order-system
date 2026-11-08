package se.cloudneutral.orders.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.api.ResourceNotFoundException;
import se.cloudneutral.orders.domain.InvalidStateTransitionException;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PaymentResult;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;
import static se.cloudneutral.orders.domain.ShipmentStatus.PAYMENT_PENDING;

@Service
public class DefaultPaymentService implements PaymentService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private PaymentClaimService paymentClaimService;

    @Autowired
    private PaymentCompletionService paymentCompletionService;

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

    // First run after one interval rather than at startup, so that a starting instance does not
    // pick up work before it has finished starting
    @Scheduled(fixedDelayString = "${app.simulation.processing-interval:PT5S}",
            initialDelayString = "${app.simulation.processing-interval:PT5S}")
    public void processPendingPayments() {
        paymentClaimService.claimNext()
                .ifPresent(request -> {
                    try {
                        PaymentResult result = paymentProcessor.process(request);
                        paymentCompletionService.complete(request.paymentId(), result);
                    } catch (Exception e) {
                        // Leave the attempt pending.
                        // It becomes eligible again when the lease expires.
                        logger.warn("Payment processing failed for {}", request.paymentId(), e);
                    }
                });

    }
}
