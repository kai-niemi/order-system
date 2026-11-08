package se.cloudneutral.orders.idempotency;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import se.cloudneutral.orders.config.IdempotencyProperties;
import se.cloudneutral.orders.repository.IdempotencyRepository;
import se.cloudneutral.orders.service.IdempotencyEvictor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit-test")
class IdempotencyEvictorTest {
    private static final int BATCH = 3;

    private IdempotencyRepository repository;

    private IdempotencyEvictor evictor;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(IdempotencyRepository.class);
        evictor = new IdempotencyEvictor();
        ReflectionTestUtils.setField(evictor, "repository", repository);
        ReflectionTestUtils.setField(evictor, "properties",
                new IdempotencyProperties(Duration.ofHours(1), null, BATCH));
    }

    @Test
    void givenNothingExpired_whenEvict_thenExpectSingleBatch() {
        when(repository.deleteExpired(BATCH)).thenReturn(0);

        evictor.evictExpired();

        verify(repository, times(1)).deleteExpired(BATCH);
    }

    @Test
    void givenMoreThanOneBatch_whenEvict_thenExpectLoopUntilShortBatch() {
        when(repository.deleteExpired(BATCH)).thenReturn(3, 3, 1);

        evictor.evictExpired();

        verify(repository, times(3)).deleteExpired(BATCH);
    }

    @Test
    void givenBatchFails_whenEvict_thenExpectNoExceptionAndNextRunContinues() {
        when(repository.deleteExpired(BATCH))
                .thenReturn(3)
                .thenThrow(new PessimisticLockingFailureException("serialization failure"))
                .thenReturn(2);

        assertThatCode(() -> evictor.evictExpired()).doesNotThrowAnyException();
        verify(repository, times(2)).deleteExpired(BATCH);

        // The next scheduled run starts over and completes
        evictor.evictExpired();
        verify(repository, times(3)).deleteExpired(BATCH);
        verify(repository, never()).deleteExpired(0);
    }

    @Test
    void givenNoConfiguredInterval_whenCreateProperties_thenExpectDefaults() {
        IdempotencyProperties properties = new IdempotencyProperties(Duration.ofHours(1), null, null);

        assertThat(properties.evictInterval()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.evictBatch()).isEqualTo(1000);
    }
}
