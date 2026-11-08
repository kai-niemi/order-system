package se.cloudneutral.orders.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import se.cloudneutral.orders.domain.IdempotencyRecord;
import se.cloudneutral.orders.order.AbstractDatabaseTest;
import se.cloudneutral.orders.repository.IdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The claim and eviction SQL, run against the reference database.
 */
public class IdempotencyRepositoryTest extends AbstractDatabaseTest {
    private static final String SCOPE = "alice";

    @Autowired
    private IdempotencyRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static byte[] fingerprint(int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return bytes;
    }

    private static Instant live() {
        return Instant.now().plus(Duration.ofHours(1));
    }

    private static Instant expired() {
        return Instant.now().minus(Duration.ofHours(1));
    }

    @Test
    void givenNewKey_whenAcquire_thenExpectSuppliedResourceIdAndStoredRecord() {
        UUID resourceId = UUID.randomUUID();

        assertThat(repository.acquire(SCOPE, "k1", fingerprint(1), resourceId, live()))
                .contains(resourceId);

        IdempotencyRecord record = repository.findByKey(SCOPE, "k1").orElseThrow();
        assertThat(record.resourceId()).isEqualTo(resourceId);
        assertThat(record.fingerprint()).isEqualTo(fingerprint(1));
        assertThat(record.createdAt()).isNotNull();
        assertThat(record.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void givenLiveRecord_whenAcquireSameKey_thenExpectEmptyAndRecordUnchanged() {
        UUID first = UUID.randomUUID();
        repository.acquire(SCOPE, "k1", fingerprint(1), first, live());

        assertThat(repository.acquire(SCOPE, "k1", fingerprint(2), UUID.randomUUID(), live()))
                .isEmpty();

        IdempotencyRecord record = repository.findByKey(SCOPE, "k1").orElseThrow();
        assertThat(record.resourceId()).isEqualTo(first);
        assertThat(record.fingerprint()).isEqualTo(fingerprint(1));
    }

    @Test
    void givenExpiredRecord_whenAcquireSameKey_thenExpectRecycledInPlace() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        repository.acquire(SCOPE, "k1", fingerprint(1), first, expired());

        assertThat(repository.acquire(SCOPE, "k1", fingerprint(2), second, live()))
                .contains(second);

        IdempotencyRecord record = repository.findByKey(SCOPE, "k1").orElseThrow();
        assertThat(record.resourceId()).isEqualTo(second);
        assertThat(record.fingerprint()).isEqualTo(fingerprint(2));
        assertThat(record.expiresAt()).isAfter(Instant.now());
        assertThat(jdbcTemplate.queryForObject("select count(*) from idempotency_record", Long.class))
                .isEqualTo(1);
    }

    @Test
    void givenSameKeyInDifferentScopes_whenAcquire_thenExpectBothToSucceed() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertThat(repository.acquire("alice", "k1", fingerprint(1), a, live())).contains(a);
        assertThat(repository.acquire("bob", "k1", fingerprint(1), b, live())).contains(b);

        assertThat(repository.findByKey("alice", "k1").orElseThrow().resourceId()).isEqualTo(a);
        assertThat(repository.findByKey("bob", "k1").orElseThrow().resourceId()).isEqualTo(b);
    }

    @Test
    void givenUnknownKey_whenFind_thenExpectEmpty() {
        assertThat(repository.findByKey(SCOPE, "missing")).isEmpty();
    }

    @Test
    void givenExpiredAndLiveRecords_whenDeleteExpired_thenExpectOnlyExpiredDeletedInBatches() {
        for (int i = 0; i < 5; i++) {
            repository.acquire(SCOPE, "expired-" + i, fingerprint(i), UUID.randomUUID(), expired());
        }
        repository.acquire(SCOPE, "live-1", fingerprint(1), UUID.randomUUID(), live());
        repository.acquire(SCOPE, "live-2", fingerprint(2), UUID.randomUUID(), live());

        assertThat(repository.deleteExpired(3)).isEqualTo(3);
        assertThat(repository.deleteExpired(3)).isEqualTo(2);
        assertThat(repository.deleteExpired(3)).isZero();

        assertThat(repository.findByKey(SCOPE, "live-1")).isPresent();
        assertThat(repository.findByKey(SCOPE, "live-2")).isPresent();
        assertThat(jdbcTemplate.queryForObject("select count(*) from idempotency_record", Long.class))
                .isEqualTo(2);
    }

    @Test
    void givenExpiredRecordLockedByAnotherTransaction_whenDeleteExpired_thenExpectItSkippedWithoutBlocking() {
        repository.acquire(SCOPE, "locked", fingerprint(1), UUID.randomUUID(), expired());
        repository.acquire(SCOPE, "free-1", fingerprint(2), UUID.randomUUID(), expired());
        repository.acquire(SCOPE, "free-2", fingerprint(3), UUID.randomUUID(), expired());

        AtomicInteger deleted = new AtomicInteger();

        // deleteExpired runs in a transaction of its own, so it does not see the lock held here
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.queryForList(
                    "select 1 from idempotency_record where scope = ? and idempotency_key = ? for update",
                    SCOPE, "locked");
            deleted.set(repository.deleteExpired(10));
        });

        assertThat(deleted.get()).isEqualTo(2);
        assertThat(repository.findByKey(SCOPE, "locked")).isPresent();
        assertThat(repository.findByKey(SCOPE, "free-1")).isEmpty();
        assertThat(repository.findByKey(SCOPE, "free-2")).isEmpty();
    }
}
