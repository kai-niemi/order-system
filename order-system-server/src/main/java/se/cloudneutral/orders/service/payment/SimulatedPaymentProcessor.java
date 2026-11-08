package se.cloudneutral.orders.service.payment;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.annotation.TransactionNever;
import se.cloudneutral.orders.domain.PaymentRequest;
import se.cloudneutral.orders.domain.PaymentResult;

@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<UUID, ProcessedPayment> payments =
            new ConcurrentHashMap<>();

    @Override
    @TransactionNever
    public PaymentResult process(PaymentRequest request) {
        if (request.simulateTimeout()) {
            logger.warn("Simulating 30s timeout for payment %s".formatted(request.paymentId()));
            simulateLatency(Duration.ofSeconds(30));
        } else {
            simulateLatency(Duration.ofMillis(ThreadLocalRandom.current().nextInt(400, 600)));
        }

        ProcessedPayment payment = payments.compute(
                request.paymentId(),
                (id, existing) -> {

                    if (existing != null) {
                        validateRequest(existing.request(), request);
                        return existing;
                    }

                    PaymentResult result =
                            ThreadLocalRandom.current().nextDouble() < 0.10
                                    ? PaymentResult.declined()
                                    : PaymentResult.succeeded("sim-" + id);

                    return new ProcessedPayment(request, result);
                }
        );

        return payment.result();
    }

    private void simulateLatency(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Payment processing interrupted", e
            );
        }
    }

    private void validateRequest(
            PaymentRequest original,
            PaymentRequest retry) {

        if (!original.equals(retry)) {
            throw new IllegalArgumentException(
                    "Payment ID reused with different parameters"
            );
        }
    }

    private record ProcessedPayment(
            PaymentRequest request,
            PaymentResult result
    ) {
    }
}