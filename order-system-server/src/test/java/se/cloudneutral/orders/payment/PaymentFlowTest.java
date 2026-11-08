package se.cloudneutral.orders.payment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import se.cloudneutral.orders.api.ResourceNotFoundException;
import se.cloudneutral.orders.domain.InvalidStateTransitionException;
import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.domain.PaymentResult;
import se.cloudneutral.orders.order.AbstractDatabaseTest;
import se.cloudneutral.orders.service.DefaultPaymentService;
import se.cloudneutral.orders.service.PaymentClaimService;
import se.cloudneutral.orders.service.PaymentCompletionService;
import se.cloudneutral.orders.service.PaymentProcessor;
import se.cloudneutral.orders.service.PaymentService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Payment initiation, claiming under a lease and completion, against the reference database. The
 * provider is replaced by a mock so that each test controls the outcome.
 * <p>
 * The base class restricts the test execution listeners, so the one that injects bean overrides
 * is added here.
 */
@TestExecutionListeners(listeners = BeanOverrideTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class PaymentFlowTest extends AbstractDatabaseTest {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DefaultPaymentService defaultPaymentService;

    @Autowired
    private PaymentClaimService claimService;

    @Autowired
    private PaymentCompletionService completionService;

    @MockitoBean
    private PaymentProcessor paymentProcessor;

    // The listener that resets mocks after each test is not among the restricted listeners of the base class
    @BeforeEach
    void resetProvider() {
        Mockito.reset(paymentProcessor);
    }

    private UUID initiate(UUID orderId) {
        UUID paymentId = UUID.randomUUID();
        paymentService.initiatePayment(orderId, paymentId, false);
        return paymentId;
    }

    private String attemptStatus(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "select status::text from payment_attempt where id = ?", String.class, paymentId);
    }

    private int attemptCount(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "select attempt_count from payment_attempt where id = ?", Integer.class, paymentId);
    }

    private long attemptRows(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from payment_attempt where order_id = ?", Long.class, orderId);
    }

    private void expireLease(UUID paymentId) {
        jdbcTemplate.update(
                "update payment_attempt set lease_until = now() - interval '1 minute' where id = ?", paymentId);
    }

    // Initiation

    @Test
    void givenPlacedOrder_whenInitiatePayment_thenExpectPendingAttemptAndOrderPaymentPending() {
        UUID orderId = placeOrder();

        UUID paymentId = initiate(orderId);

        assertThat(orderStatus(orderId)).isEqualTo("PAYMENT_PENDING");
        assertThat(attemptStatus(paymentId)).isEqualTo("PENDING");
        assertThat(attemptRows(orderId)).isEqualTo(1);
    }

    @Test
    void givenCancelledOrder_whenInitiatePayment_thenExpectInvalidStateAndNoAttempt() {
        UUID orderId = placeOrder();
        assertThat(orderService.cancelOrder(orderId)).isTrue();

        assertThatThrownBy(() -> initiate(orderId))
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(attemptRows(orderId)).isZero();
    }

    @Test
    void givenPaymentAlreadyPending_whenInitiatePaymentAgain_thenExpectInvalidState() {
        UUID orderId = placeOrder();
        initiate(orderId);

        assertThatThrownBy(() -> initiate(orderId))
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThat(attemptRows(orderId)).isEqualTo(1);
    }

    @Test
    void givenUnknownOrder_whenInitiatePayment_thenExpectNotFound() {
        assertThatThrownBy(() -> initiate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenConcurrentPaymentsForSameOrder_whenInitiate_thenExpectExactlyOneAttempt() throws Exception {
        UUID orderId = placeOrder();
        int threads = 4;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);

            List<Future<Throwable>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        initiate(orderId);
                        return null;
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }

            ready.await();
            start.countDown();

            List<Throwable> failures = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                Throwable failure = future.get(60, TimeUnit.SECONDS);
                if (failure != null) {
                    failures.add(failure);
                }
            }

            assertThat(failures).hasSize(threads - 1)
                    .allSatisfy(t -> assertThat(t).isInstanceOf(InvalidStateTransitionException.class));
            assertThat(attemptRows(orderId)).isEqualTo(1);
            assertThat(orderStatus(orderId)).isEqualTo("PAYMENT_PENDING");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void givenConcurrentPayAndCancel_whenBothRun_thenExpectConsistentFinalState() throws Exception {
        UUID orderId = placeOrder();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);

            Future<Boolean> pay = pool.submit(() -> {
                start.await();
                try {
                    initiate(orderId);
                    return true;
                } catch (InvalidStateTransitionException e) {
                    return false;
                }
            });
            Future<Boolean> cancel = pool.submit(() -> {
                start.await();
                return orderService.cancelOrder(orderId);
            });

            start.countDown();
            boolean paid = pay.get(60, TimeUnit.SECONDS);
            boolean cancelled = cancel.get(60, TimeUnit.SECONDS);

            // Exactly one of them wins, and a cancelled order never has a pending payment
            assertThat(paid ^ cancelled).isTrue();
            if (cancelled) {
                assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
                assertThat(attemptRows(orderId)).isZero();
            } else {
                assertThat(orderStatus(orderId)).isEqualTo("PAYMENT_PENDING");
                assertThat(attemptRows(orderId)).isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // Completion

    @Test
    void givenPendingPayment_whenCompleteSucceeded_thenExpectAttemptSucceededAndOrderPaid() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        completionService.complete(paymentId, PaymentResult.succeeded("ref-1"));

        assertThat(attemptStatus(paymentId)).isEqualTo("SUCCEEDED");
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject(
                "select provider_reference from payment_attempt where id = ?", String.class, paymentId))
                .isEqualTo("ref-1");
    }

    @Test
    void givenPendingPayment_whenCompleteDeclined_thenExpectAttemptDeclinedAndOrderPayableAgain() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        completionService.complete(paymentId, PaymentResult.declined());

        assertThat(attemptStatus(paymentId)).isEqualTo("DECLINED");
        assertThat(orderStatus(orderId)).isEqualTo("PLACED");

        // A new attempt is allowed once the previous one is no longer pending
        UUID second = initiate(orderId);
        assertThat(attemptStatus(second)).isEqualTo("PENDING");
        assertThat(attemptRows(orderId)).isEqualTo(2);
    }

    @Test
    void givenCompletedPayment_whenCompleteAgain_thenExpectNoOpEvenWithOtherResult() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);
        completionService.complete(paymentId, PaymentResult.succeeded("ref-1"));

        completionService.complete(paymentId, PaymentResult.succeeded("ref-1"));
        completionService.complete(paymentId, PaymentResult.declined());

        assertThat(attemptStatus(paymentId)).isEqualTo("SUCCEEDED");
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
    }

    @Test
    void givenUnknownPayment_whenComplete_thenExpectFailure() {
        assertThatThrownBy(() -> completionService.complete(UUID.randomUUID(), PaymentResult.declined()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Lease

    @Test
    void givenPendingPayment_whenClaimNext_thenExpectLeaseHeldUntilExpiry() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        Optional<PaymentRequest> first = claimService.claimNext();
        assertThat(first).isPresent();
        assertThat(first.get().paymentId()).isEqualTo(paymentId);
        assertThat(attemptCount(paymentId)).isEqualTo(1);

        // Leased, so it is not handed out again
        assertThat(claimService.claimNext()).isEmpty();

        // Once the lease expires another worker may take it over
        expireLease(paymentId);
        Optional<PaymentRequest> second = claimService.claimNext();
        assertThat(second).isPresent();
        assertThat(second.get().paymentId()).isEqualTo(paymentId);
        assertThat(attemptCount(paymentId)).isEqualTo(2);
    }

    @Test
    void givenNoPendingPayment_whenClaimNext_thenExpectEmpty() {
        placeOrder();

        assertThat(claimService.claimNext()).isEmpty();
    }

    @Test
    void givenCompletedPayment_whenClaimNext_thenExpectEmpty() {
        UUID paymentId = initiate(placeOrder());
        completionService.complete(paymentId, PaymentResult.succeeded("ref"));

        assertThat(claimService.claimNext()).isEmpty();
    }

    @Test
    void givenOnePendingPayment_whenTwoWorkersClaimConcurrently_thenExpectExactlyOneWins() throws Exception {
        initiate(placeOrder());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Optional<PaymentRequest>>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return claimService.claimNext();
                }));
            }
            start.countDown();

            int winners = 0;
            for (Future<Optional<PaymentRequest>> future : futures) {
                if (future.get(60, TimeUnit.SECONDS).isPresent()) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    // Fencing decision: no fencing token, completion is idempotent under the row lock

    @Test
    void givenLeaseExpiredAndTakenOver_whenBothWorkersComplete_thenExpectResultAppliedOnce() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        // Worker 1 claims, is slow, and its lease expires. Worker 2 takes over.
        PaymentRequest worker1 = claimService.claimNext().orElseThrow();
        expireLease(paymentId);
        PaymentRequest worker2 = claimService.claimNext().orElseThrow();
        assertThat(worker2.paymentId()).isEqualTo(worker1.paymentId());

        // Both obtain the same provider result, since the attempt id is the provider's idempotency key
        PaymentResult result = PaymentResult.succeeded("ref-" + paymentId);
        completionService.complete(worker2.paymentId(), result);
        completionService.complete(worker1.paymentId(), result);

        assertThat(attemptStatus(paymentId)).isEqualTo("SUCCEEDED");
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(attemptRows(orderId)).isEqualTo(1);
    }

    // Attempt cap

    @Test
    void givenAttemptsExhausted_whenClaimNext_thenExpectAttemptFailedAndOrderPayableAgain() {
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);
        jdbcTemplate.update("update payment_attempt set attempt_count = ? where id = ?",
                PaymentClaimService.MAX_ATTEMPTS, paymentId);
        expireLease(paymentId);

        assertThat(claimService.claimNext()).isEmpty();

        assertThat(attemptStatus(paymentId)).isEqualTo("FAILED");
        assertThat(orderStatus(orderId)).isEqualTo("PLACED");

        // A late result for the abandoned attempt changes nothing
        completionService.complete(paymentId, PaymentResult.succeeded("late"));
        assertThat(attemptStatus(paymentId)).isEqualTo("FAILED");
        assertThat(orderStatus(orderId)).isEqualTo("PLACED");

        // and the customer can pay again
        UUID retry = initiate(orderId);
        assertThat(attemptStatus(retry)).isEqualTo("PENDING");
    }

    @Test
    void givenAttemptsBelowCap_whenClaimNext_thenExpectStillClaimable() {
        UUID paymentId = initiate(placeOrder());
        jdbcTemplate.update("update payment_attempt set attempt_count = ? where id = ?",
                PaymentClaimService.MAX_ATTEMPTS - 1, paymentId);

        assertThat(claimService.claimNext()).isPresent();
        assertThat(attemptCount(paymentId)).isEqualTo(PaymentClaimService.MAX_ATTEMPTS);
    }

    // Worker

    @Test
    void givenProviderSucceeds_whenProcessPendingPayments_thenExpectOrderPaid() {
        when(paymentProcessor.process(any())).thenReturn(PaymentResult.succeeded("sim-ok"));
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        defaultPaymentService.processPendingPayments();

        assertThat(attemptStatus(paymentId)).isEqualTo("SUCCEEDED");
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
    }

    @Test
    void givenProviderThrows_whenProcessPendingPayments_thenExpectStillPendingAndRetriedAfterLease() {
        when(paymentProcessor.process(any()))
                .thenThrow(new IllegalStateException("provider down"))
                .thenReturn(PaymentResult.succeeded("sim-ok"));
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        defaultPaymentService.processPendingPayments();

        assertThat(attemptStatus(paymentId)).isEqualTo("PENDING");
        assertThat(orderStatus(orderId)).isEqualTo("PAYMENT_PENDING");
        assertThat(attemptCount(paymentId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select lease_until > now() from payment_attempt where id = ?", Boolean.class, paymentId))
                .isTrue();

        // Lease still held, so the next tick leaves it alone
        defaultPaymentService.processPendingPayments();
        verify(paymentProcessor, times(1)).process(any());

        expireLease(paymentId);
        defaultPaymentService.processPendingPayments();

        verify(paymentProcessor, times(2)).process(any());
        assertThat(attemptStatus(paymentId)).isEqualTo("SUCCEEDED");
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(attemptCount(paymentId)).isEqualTo(2);
    }

    @Test
    void givenProviderDeclines_whenProcessPendingPayments_thenExpectOrderPlacedAgain() {
        when(paymentProcessor.process(any())).thenReturn(PaymentResult.declined());
        UUID orderId = placeOrder();
        UUID paymentId = initiate(orderId);

        defaultPaymentService.processPendingPayments();

        assertThat(attemptStatus(paymentId)).isEqualTo("DECLINED");
        assertThat(orderStatus(orderId)).isEqualTo("PLACED");
    }
}
