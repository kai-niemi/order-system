package se.cloudneutral.orders.aspect;

import java.lang.reflect.Method;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import se.cloudneutral.orders.service.IdempotencyException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
class ExceptionClassifierTest {
    private final ExceptionClassifier classifier = new ExceptionClassifier();

    private final Method method = Object.class.getMethods()[0];

    @Test
    void givenSerializationFailure_thenExpectRetry() {
        // As thrown by the JDBC template when a serializable claim conflicts with a committed one
        SQLException cause = new SQLException("could not serialize access due to concurrent update", "40001");
        assertThat(classifier.shouldRetry(method, new CannotAcquireLockException("claim", cause))).isTrue();
    }

    @Test
    void givenOtherSqlState_thenExpectNoRetry() {
        SQLException cause = new SQLException("violates check constraint", "23514");
        assertThat(classifier.shouldRetry(method, new DataIntegrityViolationException("order", cause))).isFalse();
    }

    @Test
    void givenBusinessException_thenExpectNoRetry() {
        assertThat(classifier.shouldRetry(method, new IdempotencyException(
                IdempotencyException.Reason.FINGERPRINT_MISMATCH, "reused"))).isFalse();
        assertThat(classifier.shouldRetry(method, null)).isFalse();
    }
}
