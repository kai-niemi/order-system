package se.cloudneutral.orders.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.domain.PaymentResult;
import se.cloudneutral.orders.service.SimulatedPaymentProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit-test")
class SimulatedPaymentProcessorTest {
    private final SimulatedPaymentProcessor processor = new SimulatedPaymentProcessor();

    @Test
    void givenSamePaymentId_whenProcessedTwice_thenExpectSameResult() {
        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), false);

        PaymentResult first = processor.process(request);
        PaymentResult second = processor.process(request);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void givenSamePaymentIdWithDifferentParameters_whenProcessed_thenExpectRejected() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        processor.process(new PaymentRequest(paymentId, orderId, new BigDecimal("10.00"), false));

        assertThatThrownBy(() -> processor.process(
                new PaymentRequest(paymentId, orderId, new BigDecimal("99.00"), false)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
