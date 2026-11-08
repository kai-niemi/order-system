package se.cloudneutral.orders.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.api.ResourceNotFoundException;
import se.cloudneutral.orders.domain.InvalidStateTransitionException;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.domain.PaymentResult;
import se.cloudneutral.orders.domain.PaymentStatus;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.repository.PaymentAttemptRepository;

/**
 * Resolves payment attempts whose outcome was unknown when the retries ran out.
 * <p>
 * This is the only way out of PAYMENT_BLOCKED, and it needs a definitive result obtained from the
 * provider out of band: succeeded or declined. The attempt and the order change in one
 * transaction. A declined result returns the order to PLACED, where it can be paid again. A
 * succeeded result makes it PAID.
 * <p>
 * Repeating a reconciliation with the same result on a settled attempt is a no-op. A different
 * result is a consistency error, since the provider and the local record disagree.
 */
@Service
public class PaymentReconciliationService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PaymentAttemptRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * @param paymentId the unresolved payment attempt
     * @param result    the definitive provider result, which must be succeeded or declined
     * @throws IllegalArgumentException        if the result is not definitive
     * @throws ResourceNotFoundException       if there is no such payment attempt
     * @throws InvalidStateTransitionException if the attempt is still pending, its order is not blocked,
     *                                         or the attempt is already settled with a different result
     */
    @TransactionBoundary
    public void reconcile(UUID paymentId, PaymentResult result) {
        if (result.status() != PaymentStatus.SUCCEEDED && result.status() != PaymentStatus.DECLINED) {
            throw new IllegalArgumentException("Not a definitive result: " + result.status());
        }

        PaymentAttempt attempt = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));

        if (attempt.getStatus() == PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Payment attempt %s is still pending, not unresolved".formatted(paymentId));
        }

        applyDefinitiveResult(attempt, result);
    }

    /**
     * Applies a definitive result to an attempt that is already locked by the caller, within the
     * caller's transaction. Does not open a transaction of its own, so it can be shared with
     * {@link PaymentCompletionService}.
     * <ul>
     *     <li>UNRESOLVED: the result is applied to the attempt and its blocked order.</li>
     *     <li>Settled with the same result: a no-op.</li>
     *     <li>Settled with a different result: a consistency error, nothing is overwritten.</li>
     * </ul>
     *
     * @throws IllegalStateException           if the attempt is still pending
     * @throws InvalidStateTransitionException if the settled result differs, or the order is not blocked
     */
    void applyDefinitiveResult(PaymentAttempt attempt, PaymentResult result) {
        UUID paymentId = attempt.getId();

        switch (attempt.getStatus()) {
            case UNRESOLVED -> {
            }
            case PENDING -> throw new IllegalStateException(
                    "Payment attempt %s is still pending, not unresolved".formatted(paymentId));
            default -> {
                if (attempt.getStatus() != result.status()) {
                    // The provider result contradicts what is recorded. Never overwrite it.
                    logger.error("Payment attempt {} is already {}, conflicting result {}",
                            paymentId, attempt.getStatus(), result.status());
                    throw new InvalidStateTransitionException(
                            "Payment attempt %s is already %s, conflicting result %s".formatted(
                                    paymentId, attempt.getStatus(), result.status()));
                }
                // Already settled with the same result. Repeating it is a no-op.
                logger.info("Payment attempt {} is already {}, ignoring result", paymentId, attempt.getStatus());
                return;
            }
        }

        PurchaseOrder order = orderRepository.findByIdForUpdate(attempt.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found for payment attempt " + paymentId));

        attempt.reconcile(result);

        switch (result.status()) {
            case SUCCEEDED -> order.reconciledAsPaid();
            case DECLINED -> order.reconciledAsDeclined();
            default -> throw new IllegalStateException();
        }

        logger.info("Reconciled payment attempt {} as {}, order {} is now {}",
                paymentId, result.status(), order.getId(), order.getStatus());
    }
}
