package se.cloudneutral.orders.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_attempt")
public class PaymentAttempt extends AbstractEntity<UUID> {
    public static PaymentAttempt pending(
            UUID id,
            UUID orderId,
            BigDecimal amount,
            boolean simulateTimeout) {
        return new PaymentAttempt(id, orderId, amount, simulateTimeout);
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", length = 25, nullable = false)
    private PaymentStatus status;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "simulate_timeout", nullable = false)
    private boolean simulateTimeout;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(
            UUID id,
            UUID orderId,
            BigDecimal amount,
            boolean simulateTimeout) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.simulateTimeout = simulateTimeout;
    }

    public PaymentRequest toRequest() {
        return new PaymentRequest(
                id,
                orderId,
                amount,
                simulateTimeout
        );
    }

    public void claim(Instant now, Duration leaseDuration) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not pending");
        }

        leaseUntil = now.plus(leaseDuration);
        attemptCount++;
    }

    public void complete(PaymentResult result) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment attempt already completed"
            );
        }

        switch (result.status()) {
            case SUCCEEDED -> {
                status = PaymentStatus.SUCCEEDED;
                providerReference = result.providerReference();
            }
            case DECLINED -> status = PaymentStatus.DECLINED;
        }

        leaseUntil = null;
    }

    /**
     * Gives up on an attempt that exhausted its processing attempts without a provider result.
     */
    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment attempt already completed"
            );
        }

        status = PaymentStatus.FAILED;
        leaseUntil = null;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public UUID getId() {
        return id;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getProviderReference() {
        return providerReference;
    }
}