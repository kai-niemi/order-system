package se.cloudneutral.orders.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import se.cloudneutral.orders.domain.PaymentAttempt;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM PaymentAttempt p
        WHERE p.id = :id
        """)
    Optional<PaymentAttempt> findByIdForUpdate(UUID id);

    @Query(value = """
        SELECT *
        FROM payment_attempt
        WHERE status = 'PENDING'
          AND (lease_until IS NULL OR lease_until < now())
        ORDER BY id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<PaymentAttempt> findNextClaimable();
}
