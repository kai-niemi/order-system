package se.cloudneutral.orders.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;

/**
 * Hands out pending payment attempts to workers under a time limited lease.
 * <p>
 * There is deliberately no fencing token on completion. Applying a result is guarded by a row
 * lock on the attempt and a check that it is still pending, so a worker whose lease expired can
 * at worst complete an attempt that another worker also processed, and the loser is a no-op. This
 * is safe as long as the provider treats the attempt id as its idempotency key, in which case
 * both workers obtain the same result.
 */
@Service
public class PaymentClaimService {
    public static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    /**
     * Claims after which an attempt without a result is given up on, so that a permanently
     * failing attempt does not keep being retried and the order does not stay pending forever.
     */
    public static final int MAX_ATTEMPTS = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PaymentAttemptRepository repository;

    private final OrderRepository orderRepository;

    public PaymentClaimService(PaymentAttemptRepository repository, OrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    @TransactionBoundary
    public Optional<PaymentRequest> claimNext() {
        Optional<PaymentAttempt> next;

        while ((next = repository.findNextClaimable()).isPresent()) {
            PaymentAttempt attempt = next.get();

            if (attempt.getAttemptCount() >= MAX_ATTEMPTS) {
                abandon(attempt);
                continue;
            }

            attempt.claim(Instant.now(), LEASE_DURATION);
            return Optional.of(attempt.toRequest());
        }

        return Optional.empty();
    }

    // Lock order is attempt then order, same as PaymentCompletionService.
    // The order returns to PLACED so that the customer can pay again.
    private void abandon(PaymentAttempt attempt) {
        logger.warn("Giving up on payment attempt {} after {} attempts", attempt.getId(), attempt.getAttemptCount());

        orderRepository.findByIdForUpdate(attempt.getOrderId())
                .ifPresent(order -> order.paymentDeclined());
        attempt.fail();
    }
}
