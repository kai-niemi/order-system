package se.cloudneutral.orders.service.payment;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private PaymentAttemptRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentReconciliationService reconciliationService;

    /**
     * Applies a definitive provider result to a payment attempt.
     * <ul>
     *     <li>PENDING: the result is applied to the attempt and its order.</li>
     *     <li>UNRESOLVED: the retries ran out before this late result arrived. It is definitive,
     *     so it is applied like a reconciliation and the blocked order is released.</li>
     *     <li>Settled with the same result: a no-op.</li>
     *     <li>Settled with a different result: a consistency error, nothing is overwritten.</li>
     * </ul>
     *
     * @throws se.cloudneutral.orders.domain.InvalidStateTransitionException if the attempt is
     *                                                                       already settled with a different result
     */
    @TransactionBoundary
    public void complete(UUID paymentId, PaymentResult result) {
        PaymentAttempt attempt = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment attempt not found: " + paymentId)
                );

        // Not pending: a late result for an unresolved attempt, or a repeat of a settled one.
        // Runs in this transaction, under the lock already held on the attempt.
        if (attempt.getStatus() != PaymentStatus.PENDING) {
            reconciliationService.applyDefinitiveResult(attempt, result);
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