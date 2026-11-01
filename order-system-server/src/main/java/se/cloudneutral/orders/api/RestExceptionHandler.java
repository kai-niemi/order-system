package se.cloudneutral.orders.api;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import se.cloudneutral.orders.service.BusinessException;
import se.cloudneutral.orders.service.IdempotencyException;
import se.cloudneutral.orders.service.OrderService;

/**
 * Translates service and web layer exceptions to RFC 9457 problem details. Extending
 * {@link ResponseEntityExceptionHandler} replaces the handler Spring Boot otherwise registers
 * for {@code spring.mvc.problemdetails.enabled}, so the standard Spring MVC exceptions such as
 * bean validation failures keep their problem details, alongside the API's own problem types.
 *
 * @see ProblemTypes
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ProblemDetail handle(Exception ex) {
        // Nothing about the failure is exposed to the client
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missing
            && OrderController.HEADER_IDEMPOTENCY_KEY.equalsIgnoreCase(missing.getHeaderName())) {
            ProblemDetail problem = ex.getBody();
            problem.setType(ProblemTypes.IDEMPOTENCY_KEY_REQUIRED);
            problem.setTitle("Idempotency key required");
            problem.setDetail("Placing an order requires an '%s' header identifying the attempt"
                    .formatted(OrderController.HEADER_IDEMPOTENCY_KEY));
        }
        return super.handleServletRequestBindingException(ex, headers, status, request);
    }

    @ExceptionHandler(IdempotencyException.class)
    public ProblemDetail handle(IdempotencyException exception) {
        return switch (exception.getReason()) {
            case FINGERPRINT_MISMATCH -> problem(HttpStatus.UNPROCESSABLE_CONTENT,
                    ProblemTypes.IDEMPOTENCY_KEY_REUSED, "Idempotency key reused", exception.getMessage());
            case RECORD_LOST -> problem(HttpStatus.CONFLICT,
                    ProblemTypes.IDEMPOTENCY_RECORD_LOST, "Idempotency record lost", exception.getMessage());
        };
    }

    /**
     * Thrown by {@link OrderService#placeOrder} when the order
     * violates a database constraint, typically by drawing a product inventory below zero.
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        return problem(HttpStatus.CONFLICT,
                ProblemTypes.ORDER_REJECTED, "Order rejected", ex.getMessage());
    }

    @ExceptionHandler(NoSuchResourceException.class)
    public ProblemDetail handleNoSuchResource(NoSuchResourceException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT,
                ProblemTypes.UNKNOWN_REFERENCE, "Unknown resource reference", ex.getMessage());
    }

    static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }
}
