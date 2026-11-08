package se.cloudneutral.orders.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.domain.PaymentResult;

@Service
public class PaymentCompletionScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private PaymentClaimService paymentClaimService;

    @Autowired
    private PaymentCompletionService paymentCompletionService;

    // First run after one interval rather than at startup, so that a starting instance does not
    // pick up work before it has finished starting
    @Scheduled(fixedDelayString = "${app.simulation.processing-interval:PT5S}", initialDelayString = "${app.simulation.processing-interval:PT5S}")
    public void processPendingPayments() {
        paymentClaimService.claimNext()
                .ifPresent(request -> {
                    try {
                        // Counted before the call, so that a crash during it still counts
                        if (!paymentClaimService.recordProcessing(request.paymentId())) {
                            return;
                        }

                        PaymentResult result = paymentProcessor.process(request);

                        // Also settles a late result for an attempt that was blocked as unresolved
                        paymentCompletionService.complete(request.paymentId(), result);
                    } catch (Exception e) {
                        // Leave the attempt pending. It becomes eligible again when the lease
                        // expires, until the processing attempts are exhausted and it is blocked.
                        logger.warn("Payment processing failed for {}", request.paymentId(), e);
                    }
                });

    }

}
