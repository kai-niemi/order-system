package se.cloudneutral.orders.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import se.cloudneutral.orders.config.IdempotencyProperties;
import se.cloudneutral.orders.repository.IdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The safety net for a claim that fails while the conflicting record cannot be read back. The
 * database prevents this, so it can only be provoked with a repository that misbehaves.
 */
@Tag("unit-test")
class IdempotencyServiceRecordLostTest {
    private IdempotencyRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(IdempotencyRepository.class);
        service = new IdempotencyService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "properties",
                new IdempotencyProperties(Duration.ofHours(1), null, null));
    }

    @Test
    void givenClaimFailedAndRecordAbsent_whenExecute_thenExpectRecordLostWithoutRunningOperation() {
        when(repository.acquire(anyString(), anyString(), any(), any(), any())).thenReturn(Optional.empty());
        when(repository.findByKey(anyString(), anyString())).thenReturn(Optional.empty());
        AtomicBoolean operationRan = new AtomicBoolean();

        assertThatThrownBy(() -> service.execute(
                new IdempotencyRequest("alice", "k1", "POST", "/api/orders", "body"),
                UUID.randomUUID(),
                id -> operationRan.set(true),
                id -> id))
                .isInstanceOfSatisfying(IdempotencyException.class, e ->
                        assertThat(e.getReason()).isEqualTo(IdempotencyException.Reason.RECORD_LOST));

        assertThat(operationRan).isFalse();
    }
}
