package se.cloudneutral.orders.idempotency;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;

import se.cloudneutral.orders.api.CreateOrderFacade;
import se.cloudneutral.orders.api.NoSuchResourceException;
import se.cloudneutral.orders.api.payload.CreateOrderItemRequest;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;
import se.cloudneutral.orders.order.AbstractDatabaseTest;
import se.cloudneutral.orders.repository.IdempotencyRepository;
import se.cloudneutral.orders.service.IdempotencyException;
import se.cloudneutral.orders.service.IdempotencyRequest;
import se.cloudneutral.orders.service.IdempotencyResult;
import se.cloudneutral.orders.service.IdempotencyService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Claim paths of {@link IdempotencyService} with real order creation, in the same transaction as
 * the claim, against the reference database.
 */
public class IdempotencyServiceTest extends AbstractDatabaseTest {
    private static final String SCOPE = "alice";

    private static final String PATH = "/api/orders";

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private CreateOrderFacade createOrderFacade;

    private IdempotencyResult<UUID> execute(String scope, String key, CreateOrderRequest request,
                                            UUID orderId, AtomicInteger calls) {
        return idempotencyService.execute(
                new IdempotencyRequest(scope, key, "POST", PATH, request),
                orderId,
                id -> {
                    calls.incrementAndGet();
                    createOrderFacade.createOrder(id, request);
                },
                id -> id);
    }

    @Test
    void givenFirstCall_whenExecute_thenExpectOperationRunOnceAndOrderCreated() {
        AtomicInteger calls = new AtomicInteger();
        UUID orderId = UUID.randomUUID();

        IdempotencyResult<UUID> result = execute(SCOPE, "k1", orderRequest(1), orderId, calls);

        assertThat(result.replayed()).isFalse();
        assertThat(result.resource()).isEqualTo(orderId);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(orderStatus(orderId)).isEqualTo("PLACED");
        assertThat(idempotencyRepository.findByKey(SCOPE, "k1").orElseThrow().resourceId())
                .isEqualTo(orderId);
    }

    @Test
    void givenSamePayload_whenReplay_thenExpectSameResourceAndNoSecondOrder() {
        CreateOrderRequest request = orderRequest(1);
        AtomicInteger calls = new AtomicInteger();
        UUID orderId = UUID.randomUUID();

        execute(SCOPE, "k1", request, orderId, calls);
        IdempotencyResult<UUID> replay = execute(SCOPE, "k1", request, UUID.randomUUID(), calls);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.resource()).isEqualTo(orderId);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void givenDifferentPayload_whenReplay_thenExpectFingerprintMismatchAndNoSecondOrder() {
        AtomicInteger calls = new AtomicInteger();
        execute(SCOPE, "k1", orderRequest(1), UUID.randomUUID(), calls);

        assertThatThrownBy(() -> execute(SCOPE, "k1", orderRequest(2), UUID.randomUUID(), calls))
                .isInstanceOfSatisfying(IdempotencyException.class, e ->
                        assertThat(e.getReason()).isEqualTo(IdempotencyException.Reason.FINGERPRINT_MISMATCH));

        assertThat(calls.get()).isEqualTo(1);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void givenSameKeyInOtherScope_whenExecute_thenExpectIndependentOrders() {
        CreateOrderRequest request = orderRequest(1);
        AtomicInteger calls = new AtomicInteger();

        IdempotencyResult<UUID> a = execute("alice", "k1", request, UUID.randomUUID(), calls);
        IdempotencyResult<UUID> b = execute("bob", "k1", request, UUID.randomUUID(), calls);

        assertThat(a.replayed()).isFalse();
        assertThat(b.replayed()).isFalse();
        assertThat(a.resource()).isNotEqualTo(b.resource());
        assertThat(orderCount()).isEqualTo(2);
    }

    @Test
    void givenOperationFails_whenExecute_thenExpectKeyRolledBackAndRetryExecutesAgain() {
        CreateOrderRequest broken = new CreateOrderRequest(UUID.randomUUID(),
                List.of(new CreateOrderItemRequest(anyProduct().getId(), 1)));
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> execute(SCOPE, "k1", broken, UUID.randomUUID(), calls))
                .isInstanceOf(NoSuchResourceException.class);

        // The claim shared the failed transaction, so nothing is left behind
        assertThat(idempotencyRepository.findByKey(SCOPE, "k1")).isEmpty();
        assertThat(orderCount()).isZero();

        // The same key now executes again, with no mismatch against the failed attempt
        IdempotencyResult<UUID> retry = execute(SCOPE, "k1", orderRequest(1), UUID.randomUUID(), calls);

        assertThat(retry.replayed()).isFalse();
        assertThat(calls.get()).isEqualTo(2);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void givenExpiredRecord_whenExecuteSameKeyWithOtherPayload_thenExpectNewClaimNotMismatch() {
        AtomicInteger calls = new AtomicInteger();
        UUID first = UUID.randomUUID();
        execute(SCOPE, "k1", orderRequest(1), first, calls);

        jdbcTemplate.update("update idempotency_record set expires_at = now() - interval '1 hour'");

        UUID second = UUID.randomUUID();
        IdempotencyResult<UUID> result = execute(SCOPE, "k1", orderRequest(2), second, calls);

        assertThat(result.replayed()).isFalse();
        assertThat(result.resource()).isEqualTo(second);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(orderCount()).isEqualTo(2);
    }

    @Test
    void givenSerializationFailureOnFirstAttempt_whenExecute_thenExpectRetryProducesExactlyOneOrder() {
        CreateOrderRequest request = orderRequest(1);
        AtomicInteger calls = new AtomicInteger();
        UUID orderId = UUID.randomUUID();

        IdempotencyResult<UUID> result = idempotencyService.execute(
                new IdempotencyRequest(SCOPE, "k1", "POST", PATH, request),
                orderId,
                id -> {
                    if (calls.incrementAndGet() == 1) {
                        throw new PessimisticLockingFailureException("forced",
                                new SQLException("forced serialization failure", "40001"));
                    }
                    createOrderFacade.createOrder(id, request);
                },
                id -> id);

        // The first attempt rolled back with its claim, the retry claimed the key again
        assertThat(calls.get()).isEqualTo(2);
        assertThat(result.replayed()).isFalse();
        assertThat(result.resource()).isEqualTo(orderId);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void givenConcurrentRequestsWithSameKey_whenExecute_thenExpectOneOrderAndOthersReplay() throws Exception {
        int threads = 8;
        CreateOrderRequest request = orderRequest(1);
        AtomicInteger calls = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);

            List<Future<IdempotencyResult<UUID>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return execute(SCOPE, "k1", request, UUID.randomUUID(), calls);
                }));
            }

            ready.await();
            start.countDown();

            List<IdempotencyResult<UUID>> results = new ArrayList<>();
            for (Future<IdempotencyResult<UUID>> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(results).filteredOn(r -> !r.replayed()).hasSize(1);
            assertThat(results).filteredOn(IdempotencyResult::replayed).hasSize(threads - 1);
            assertThat(results).extracting(IdempotencyResult::resource).containsOnly(
                    idempotencyRepository.findByKey(SCOPE, "k1").orElseThrow().resourceId());
            assertThat(calls.get()).isEqualTo(1);
            assertThat(orderCount()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
