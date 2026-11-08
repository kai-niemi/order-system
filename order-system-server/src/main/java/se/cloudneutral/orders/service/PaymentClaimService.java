package se.cloudneutral.orders.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.domain.PaymentStatus;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;

/**
 * No fencing token is needed on completion. Row locking and a pending-state check
 * ensure an attempt is settled only once, even when leases overlap. This relies
 * on the provider returning a consistent result for the same idempotency key.
 *
 * <p>Retry limits count processing dispatches, not confirmed provider calls.
 * Exhaustion leaves the payment outcome unknown, so the order is blocked
 * until reconciliation rather than risking a second charge.
 */
@Service
public class PaymentClaimService {
    public static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    /**
     * Processor calls after which an attempt without a definitive result is parked as unresolved,
     * so that a permanently failing attempt does not keep being retried.
     */
    public static final int MAX_ATTEMPTS = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PaymentAttemptRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    @TransactionBoundary
    public Optional<PaymentRequest> claimNext() {
        Optional<PaymentAttempt> next;

        while ((next = repository.findNextClaimable()).isPresent()) {
            PaymentAttempt attempt = next.get();

            if (attempt.getAttemptCount() >= MAX_ATTEMPTS) {
                block(attempt);
                continue;
            }

            attempt.claim(Instant.now(), LEASE_DURATION);
            return Optional.of(attempt.toRequest());
        }

        return Optional.empty();
    }

    /**
     * Serializes dispatch recording with payment completion.
     *
     * @return false if processing is no longer permitted
     */
    @TransactionBoundary
    public boolean recordProcessing(UUID paymentId) {
        Optional<PaymentAttempt> attempt = repository.findByIdForUpdate(paymentId);

        if (attempt.isEmpty() || attempt.get().getStatus() != PaymentStatus.PENDING
            || attempt.get().getAttemptCount() >= MAX_ATTEMPTS) {
            return false;
        }

        attempt.get().startProcessing();
        return true;
    }

    // Maintain the same lock order as PaymentCompletionService: attempt, then order.
    private void block(PaymentAttempt attempt) {
        logger.warn("Payment attempt {} has no definitive result after {} processing attempts ({} claims), "
                    + "blocking order {}",
                attempt.getId(), attempt.getAttemptCount(), attempt.getClaimCount(), attempt.getOrderId());

        orderRepository.findByIdForUpdate(attempt.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found for payment attempt " + attempt.getId()))
                .paymentBlocked();

        attempt.markUnresolved();
    }
}
