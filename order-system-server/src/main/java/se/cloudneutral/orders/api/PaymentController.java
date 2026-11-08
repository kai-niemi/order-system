package se.cloudneutral.orders.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import se.cloudneutral.orders.api.assembler.PaymentModelAssembler;
import se.cloudneutral.orders.api.model.PaymentModel;
import se.cloudneutral.orders.domain.PaymentAttempt;
import se.cloudneutral.orders.service.IdempotencyRequest;
import se.cloudneutral.orders.service.IdempotencyResult;
import se.cloudneutral.orders.service.IdempotencyService;
import se.cloudneutral.orders.service.PaymentService;
import static se.cloudneutral.orders.api.WebConstants.ANONYMOUS_SCOPE;
import static se.cloudneutral.orders.api.WebConstants.HEADER_IDEMPOTENCY_KEY;
import static se.cloudneutral.orders.api.WebConstants.HEADER_SIMULATE_TIMEOUT;

@RestController
@RequestMapping(value = "/api/orders/{orderId}/payments",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentModelAssembler assembler;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private HttpServletRequest httpRequest;

    @PostMapping
    public ResponseEntity<PaymentModel> payOrder(
            @PathVariable UUID orderId,
            @RequestHeader(HEADER_IDEMPOTENCY_KEY) String key,
            @RequestHeader(value = HEADER_SIMULATE_TIMEOUT, required = false) String simulateTimeoutHeader) {

        String scope = httpRequest.getUserPrincipal() != null
                ? httpRequest.getUserPrincipal().getName()
                : ANONYMOUS_SCOPE;

        boolean simulateTimeout = "yes".equalsIgnoreCase(simulateTimeoutHeader);

        // Everything that determines the outcome is part of the fingerprint, so reusing a key with
        // a different order or different simulation flag is rejected instead of replayed. The path
        // carries the order id as well, the body makes the effective parameters explicit.
        Map<String, Object> fingerprintBody = Map.of("orderId", orderId.toString());

        IdempotencyResult<PaymentModel> result = idempotencyService.execute(
                new IdempotencyRequest(scope, key, httpRequest.getMethod(), httpRequest.getRequestURI(),
                        fingerprintBody),
                UUID.randomUUID(), // paymentAttemptId passed to operation
                paymentAttemptId -> paymentService.initiatePayment(orderId, paymentAttemptId, simulateTimeout),
                this::findPaymentModel
        );

        PaymentModel model = result.resource();

        Link selfLink = model.getRequiredLink(IanaLinkRelations.SELF);

        return ResponseEntity
                .accepted()
                .location(selfLink.toUri())
                .header(WebConstants.HEADER_IDEMPOTENCY_REPLAYED, Boolean.toString(result.replayed()))
                .body(model);
    }

    private PaymentModel findPaymentModel(UUID id) {
        PaymentAttempt payment =
                paymentService.getPaymentAttempt(id)
                        .orElseThrow(() -> ResourceNotFoundException.payment(id));
        return assembler.toModel(payment);
    }

    @GetMapping("/{paymentId}")
    public PaymentModel getPayment(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("paymentId") UUID paymentId) {
        // The attempt must belong to the order in the path, otherwise it is not found under it
        PaymentAttempt payment = paymentService.getPaymentAttempt(paymentId)
                .filter(attempt -> attempt.getOrderId().equals(orderId))
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));
        return assembler.toModel(payment);
    }
}
