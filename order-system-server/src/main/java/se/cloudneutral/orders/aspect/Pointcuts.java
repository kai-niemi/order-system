package se.cloudneutral.orders.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.resilience.annotation.Retryable;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.annotation.TransactionNotSupported;

/**
 * Various domain-agnostic pointcut expression for retry/decoration aspects.
 *
 * @author Kai Niemi
 */
@Aspect
public class Pointcuts {
    /**
     * Pointcut expression matching all transactional boundaries.
     */
    @Pointcut("execution(public * *(..)) && @annotation(transactionBoundary)")
    public void anyExplicitTransactionBoundary(TransactionBoundary transactionBoundary) {
    }

    /**
     * Pointcut expression matching all non-transactional operations.
     */
    @Pointcut("execution(public * *(..)) && @annotation(transactionNotSupported)")
    public void anyImplicitTransactionOperation(TransactionNotSupported transactionNotSupported) {
    }

    /**
     * Pointcut expression matching all retryable operations.
     */
    @Pointcut("execution(public * *(..)) && @annotation(retryable)")
    public void anyRetryableOperation(Retryable retryable) {
    }
}

